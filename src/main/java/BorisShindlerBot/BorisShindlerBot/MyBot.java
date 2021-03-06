package BorisShindlerBot.BorisShindlerBot;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class MyBot extends TelegramLongPollingBot {

	
	private final String token;
	private final UserSet usetSet;
	private final String broadcastPassword;
	// executorDB threads size should be 1 for thread safety.
	private final ThreadPoolExecutor executorDB = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
	private final ThreadPoolExecutor executorMessages = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

	public MyBot(String token, UserSet usetSet, String broadcastPassword) {
		this.token = token;
		this.usetSet = usetSet;
		this.broadcastPassword = broadcastPassword + " ";
	}

	@Override
	public String getBotToken() {
		return token;
	}
	
	private String stringIfNull(String s) {
		if (s == null) {
			return "null";
		}
		return s;
	}

	@SuppressWarnings("unchecked")
	public void onUpdateReceived(Update update) {
		executorMessages.execute(() -> {
			if (update.getMessage().getFrom().getBot()) {
				// message sent by a bot.
				System.err.println(update);
				return;
			}
			// insert to database
			executorDB.execute(() -> {
		        if(usetSet.addToSet(update)){
		    		System.out.println(update);
		            String firstName = stringIfNull(update.getMessage().getFrom().getFirstName());
					String userName = stringIfNull(update.getMessage().getFrom().getUserName());
					String lastName = stringIfNull(update.getMessage().getFrom().getLastName());
					lastName = lastName + " (" + userName + ")";
					usetSet.inserInDB(update.getMessage().getChatId(), update.getMessage().getFrom().getId(),
		                    firstName, lastName);
		        }			
			});
			if (update.hasMessage() && update.getMessage().hasText()) {
				// Set variables
				String message_text = update.getMessage().getText();
				Object sendMessage;
				if (message_text.equals("/start")) {
					sendMessage = startAction(update.getMessage());
				} else if (null != (sendMessage = Actions.getAction(message_text, update.getMessage()))) {
					
				} else if (message_text.contains("@")) {
					sendMessage = Actions.getAction("@", update.getMessage());
				}
				else if (message_text.startsWith(broadcastPassword)) {
					sendMessage = broadcastAction(update.getMessage());
				}
				else {
					sendMessage = startAction(update.getMessage());
				}
				try {
					if (sendMessage instanceof Object[]) {
						for(Object m: (Object[])sendMessage) {
							doSend(m);
						}
					} else {
						doSend(sendMessage);
					}
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
	
			}
		});
	}

	private Object broadcastAction(Message message) {
		String outText = message.getText().substring(broadcastPassword.length());
		SendMessage outMessage;
		System.out.println("Broadcasting Mesage: " + outText);
		int i = 0;
		if (outText.length() > 1) {
			for (String chat_id: usetSet.getChatIds()) {
				outMessage = new SendMessage()
						.setChatId(chat_id).setText(outText);
				outMessage.enableMarkdown(true);
				try {
					doSend(outMessage);
					i++;
				} catch (Throwable e) {
					System.err.println("Broadcasting Mesage failed for chat_id: " + chat_id);
				}
			}
		}
		outText = "_Broadcasting Mesage been sent for : " + i + "/" + usetSet.getChatIds().size() + " users_";
		outMessage = new SendMessage()
				.setChatId(message.getChatId()).setText(outText);
		outMessage.enableMarkdown(true);
		System.out.println(outText);
		return outMessage;
	}

	private void doSend(Object sendMessage) throws TelegramApiException {
		if (sendMessage == null) {
			return;
		}
		if (sendMessage instanceof BotApiMethod) {
			execute((BotApiMethod) sendMessage); // Call method to send the message
		} else {
			sendDocument((SendDocument)sendMessage);
		}
	}

	private SendMessage startAction(Message m) {
		String[][] rows = new String[][] {
			{"Жилье","Арнона", "Транспорт"},
			{"Разрешение на парковку", "Учеба", "Здоровье"},
				{"О Боте"}
		}; 
		SendMessage message = Utils.createSendMessage(m, rows);
		return message;
	}

	public String getBotUsername() {
		return "TLVBotBorisShindler";
	}

}
