package org.whixard;

import it.tdlight.client.APIToken;
import it.tdlight.client.AuthenticationData;
import it.tdlight.client.CommandHandler;
import it.tdlight.client.Result;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.client.TDLibSettings;
import it.tdlight.common.Init;
import it.tdlight.common.utils.CantLoadLibrary;
import it.tdlight.jni.TdApi;
import it.tdlight.jni.TdApi.AuthorizationState;
import it.tdlight.jni.TdApi.Chat;
import it.tdlight.jni.TdApi.MessageContent;
import it.tdlight.jni.TdApi.MessageSender;
import it.tdlight.jni.TdApi.MessageSenderUser;
import it.tdlight.jni.TdApi.MessageText;
import it.tdlight.jni.TdApi.UpdateAuthorizationState;
import it.tdlight.jni.TdApi.UpdateNewMessage;

import java.util.Scanner;

public final class App {
    public static void main(String[] args) throws CantLoadLibrary, InterruptedException {

        Scanner sc = new Scanner(System.in);
        System.out.print("Enter chatID of the chat you want to log: ");
        long chatID = sc.nextLong();

        System.out.println("Enter chatID of the log chat: ");
        long logID = sc.nextLong();
        sc.close();

        // Initialize TDLight native libraries
        Init.start();

        // Obtain the API token
        APIToken apiToken = APIToken.example();

        // Configure the client
        TDLibSettings settings = TDLibSettings.create(apiToken);

        // Create a client
        SimpleTelegramClient client = new SimpleTelegramClient(settings);

        // Configure the authentication info
        AuthenticationData authenticationData = AuthenticationData.consoleLogin();

        // Add an example update handler that prints when the bot is started
        client.addUpdateHandler(UpdateAuthorizationState.class, update -> printStatus(update.authorizationState));

        client.addUpdateHandler(UpdateAuthorizationState.class, update -> {
            if (update.authorizationState instanceof TdApi.AuthorizationStateReady) {
                client.send(new TdApi.LoadChats(new TdApi.ChatListMain(), Integer.MAX_VALUE), result -> {

                });
            }
        });

        // Add an example update handler that prints every received message
        client.addUpdateHandler(UpdateNewMessage.class, update -> {
            if (update.message.chatId != chatID) {
                return;
            }
            // Get the message content
            MessageContent messageContent = update.message.content;

            // Get the message text
            String text;
            if (messageContent instanceof MessageText) {
                // Get the text of the text message
                text = ((MessageText) messageContent).text.text;
            } else {
                // We handle only text messages, the other messages will be printed as their type
                text = "(" + messageContent.getClass().getSimpleName() + ")";
            }

            // Get the chat title
            client.send(new TdApi.GetChat(update.message.chatId), (Result<Chat> chatIdResult) -> {
                // Get the chat response
                Chat chat = chatIdResult.get();
                var sender = update.message.sender;
                if (sender instanceof MessageSenderUser) { 
                    var senderUser = (MessageSenderUser) sender;
                    client.send(new TdApi.GetUser(senderUser.userId), (Result<TdApi.User> userName) -> {
                        String chatName = chat.title;
                        String profileName = userName.get().username;
                        System.out.println("Received new message from chat " + chatName + ": " + profileName + ": " + text);
                        Result<TdApi.FormattedText> parsedText = client.execute(new TdApi.ParseTextEntities(chatName + ": " + "<b>" + profileName + "</b>" + ": " + text, new TdApi.TextParseModeHTML()));
                        var logmessage = new TdApi.SendMessage();
                        logmessage.chatId = logID;
                        var input = new TdApi.InputMessageText();
                        input.text = parsedText.get();
                        logmessage.inputMessageContent = input;
                        logmessage.options = new TdApi.MessageSendOptions();
                        client.send(logmessage, (Result<TdApi.Message> messageResult) -> {
                            messageResult.get();
                        });
                    });
                }
                ;

            });
        });

        // Start the client
        client.start(authenticationData);

        // Wait for exit
        client.waitForExit();
    }

    /**
     * Print the bot status
     */
    private static void printStatus(AuthorizationState authorizationState) {
        if (authorizationState instanceof TdApi.AuthorizationStateReady) {
            System.out.println("Logged in");
        } else if (authorizationState instanceof TdApi.AuthorizationStateClosing) {
            System.out.println("Closing...");
        } else if (authorizationState instanceof TdApi.AuthorizationStateClosed) {
            System.out.println("Closed");
        } else if (authorizationState instanceof TdApi.AuthorizationStateLoggingOut) {
            System.out.println("Logging out...");
        }
    }
}
