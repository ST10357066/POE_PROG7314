const admin = require("firebase-admin");
// --- 1. USE THE CORRECT MODERN IMPORT ---
const { getMessaging } = require("firebase-admin/messaging");

// Serverless-safe initialization
if (!admin.apps.length) {
  const serviceAccountJson = Buffer.from(
    process.env.FIREBASE_SERVICE_ACCOUNT_BASE64, "base64"
  ).toString("utf-8");
  const serviceAccount = JSON.parse(serviceAccountJson);
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
}

const db = admin.firestore();
// --- 2. USE THE CORRECT MODERN INITIALIZATION ---
const messaging = getMessaging();

module.exports = async (req, res) => {
    console.log("-> Cron job: Starting to check for task reminders...");

    // Security check
    const secret = req.headers.authorization?.split("Bearer ")[1];
    if (secret !== process.env.VERCEL_CRON_SECRET) {
        return res.status(401).send('Unauthorized');
    }

    try {
        const now = new Date();
        const oneHourFromNow = new Date(now.getTime() + 60 * 60 * 1000);

        const querySnapshot = await db.collection('tasks')
            .where('isDone', '==', false)
            .where('dueDate', '>=', now.toISOString())
            .where('dueDate', '<', oneHourFromNow.toISOString())
            .get();

        if (querySnapshot.empty) {
            console.log("   - No tasks found due in the next hour.");
            return res.status(200).send("No tasks to notify.");
        }

        console.log(`   - Found ${querySnapshot.size} tasks due soon.`);
        
        for (const taskDoc of querySnapshot.docs) {
            const task = taskDoc.data();
            const userId = task.userId;
            
            const tokenSnapshot = await db.collection('fcmTokens')
                .where('userId', '==', userId)
                .get();

            if (tokenSnapshot.empty) {
                console.log(`   - No tokens for user ${userId}. Skipping.`);
                continue;
            }

            const tokens = tokenSnapshot.docs.map(doc => doc.data().token);

            // --- 3. USE THE CORRECT MODERN 'sendEachForMulticast' METHOD ---
            const message = {
                notification: {
                    title: 'Task Reminder: TaskMaster',
                    body: `Your task "${task.title}" is due soon!`,
                },
                // The 'sendEachForMulticast' function takes the tokens inside the message payload
                tokens: tokens,
            };

            const response = await messaging.sendEachForMulticast(message);
            // --- END OF CHANGE ---

            console.log(`   - Sent notification for task "${task.title}". Success: ${response.successCount}, Failure: ${response.failureCount}`);

            if (response.failureCount > 0) {
                const staleTokens = [];
                response.responses.forEach((resp, idx) => {
                    if (!resp.success) {
                        const errorCode = resp.error.code;
                        console.error(`   - Failure for token ${tokens[idx]}:`, errorCode);
                        // Check for the specific error code indicating an invalid token
                        if (errorCode === 'messaging/registration-token-not-registered' ||
                            errorCode === 'messaging/invalid-registration-token') {
                            staleTokens.push(tokens[idx]);
                        }
                    }
                });

                // Batch delete any stale tokens found
                if (staleTokens.length > 0) {
                    console.log(`   - Deleting ${staleTokens.length} stale tokens.`);
                    const batch = db.batch();
                    staleTokens.forEach(token => {
                        const tokenRef = db.collection('fcmTokens').doc(token);
                        batch.delete(tokenRef);
                    });
                    await batch.commit();
                }
            }
        }

        res.status(200).send("Reminders sent successfully.");

    } catch (error) {
        console.error("!!! CRON JOB ERROR:", error);
        res.status(500).send({ error: "Error running cron job.", details: error.message });
    }
};