/**
 * @(#) LineController.class $version 2017. 03. 29
 * <p>
 * Copyright 2017 NAVER Corp. All rights Reserved.
 * NAVER PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.suhyunkim.ticket.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.message.ImagemapMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.imagemap.ImagemapArea;
import com.linecorp.bot.model.message.imagemap.ImagemapBaseSize;
import com.linecorp.bot.model.message.imagemap.MessageImagemapAction;
import com.linecorp.bot.model.message.imagemap.URIImagemapAction;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

import lombok.NonNull;

/**
 * LineController
 *
 * @author Suhyun Kim (k.i.m@navercorp.com)
 */
@LineMessageHandler
public class TicketBotController {

	@Autowired private LineMessagingClient lineMessagingClient;

	@EventMapping public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
		TextMessageContent message = event.getMessage();
		handleTextContent(event.getReplyToken(), event, message);
	}

	private void reply(@NonNull String replyToken, @NonNull Message message) {
		reply(replyToken, Collections.singletonList(message));
	}

	private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
		try {
			BotApiResponse apiResponse = lineMessagingClient.replyMessage(new ReplyMessage(replyToken, messages)).get();
			//		log.info("Sent Sentmessages: {}", apiResponse);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private void replyText(@NonNull String replyToken, @NonNull String message) {
		if (replyToken.isEmpty()) {
			throw new IllegalArgumentException("replyToken must not be empty");
		}
		if (message.length() > 1000) {
			message = message.substring(0, 1000 - 2) + "……";
		}
		this.reply(replyToken, new TextMessage(message));
	}

	private void handleTextContent(String replyToken, Event event, TextMessageContent content) throws Exception {
		String text = content.getText();

		//		log.info("Got text message from {}: {}", replyToken, text);
		switch (text) {
			case "profile": {
				String userId = event.getSource().getUserId();
				if (userId != null) {
					lineMessagingClient.getProfile(userId).whenComplete((profile, throwable) -> {
						if (throwable != null) {
							this.replyText(replyToken, throwable.getMessage());
							return;
						}

						this.reply(replyToken, Arrays.asList(new TextMessage("Display name: " + profile.getDisplayName()), new TextMessage("Status message: " + profile.getStatusMessage())));

					});
				} else {
					this.replyText(replyToken, "Bot can't use profile API without user ID");
				}
				break;
			}
			case "bye": {
				Source source = event.getSource();
				if (source instanceof GroupSource) {
					this.replyText(replyToken, "Leaving group");
					lineMessagingClient.leaveGroup(((GroupSource)source).getGroupId()).get();
				} else if (source instanceof RoomSource) {
					this.replyText(replyToken, "Leaving room");
					lineMessagingClient.leaveRoom(((RoomSource)source).getRoomId()).get();
				} else {
					this.replyText(replyToken, "Bot can't leave from 1:1 chat");
				}
				break;
			}
			case "confirm": {
				ConfirmTemplate confirmTemplate = new ConfirmTemplate("Do it?", new MessageAction("Yes", "Yes!"), new MessageAction("No", "No!"));
				TemplateMessage templateMessage = new TemplateMessage("Confirm alt text", confirmTemplate);
				this.reply(replyToken, templateMessage);
				break;
			}
			case "buttons": {
				String imageUrl = createUri("/static/buttons/1040.jpg");
				ButtonsTemplate buttonsTemplate = new ButtonsTemplate(imageUrl, "My button sample", "Hello, my button", Arrays.asList(new URIAction("Go to line.me", "https://line.me"), new PostbackAction("Say hello1", "hello こんにちは"), new PostbackAction("言 hello2", "hello こんにちは", "hello こんにちは"), new MessageAction("Say message", "Rice=米")));
				TemplateMessage templateMessage = new TemplateMessage("Button alt text", buttonsTemplate);
				this.reply(replyToken, templateMessage);
				break;
			}
			case "carousel": {
				String imageUrl = createUri("/static/buttons/1040.jpg");
				CarouselTemplate carouselTemplate = new CarouselTemplate(Arrays.asList(new CarouselColumn(imageUrl, "hoge", "fuga", Arrays.asList(new URIAction("Go to line.me", "https://line.me"), new PostbackAction("Say hello1", "hello こんにちは"))), new CarouselColumn(imageUrl, "hoge", "fuga", Arrays.asList(new PostbackAction("言 hello2", "hello こんにちは", "hello こんにちは"), new MessageAction("Say message", "Rice=米")))));
				TemplateMessage templateMessage = new TemplateMessage("Carousel alt text", carouselTemplate);
				this.reply(replyToken, templateMessage);
				break;
			}
			case "imagemap":
				this.reply(replyToken, new ImagemapMessage(createUri("/static/rich"), "This is alt text", new ImagemapBaseSize(1040, 1040),
					Arrays.asList(new URIImagemapAction("https://store.line.me/family/manga/en", new ImagemapArea(0, 0, 520, 520)), new URIImagemapAction("https://store.line.me/family/music/en", new ImagemapArea(520, 0, 520, 520)), new URIImagemapAction("https://store.line.me/family/play/en", new ImagemapArea(0, 520, 520, 520)), new MessageImagemapAction("URANAI!", new ImagemapArea(520, 520, 520, 520)))));
				break;
			default:
				//				log.info("Returns echo message {}: {}", replyToken, text);
				this.replyText(replyToken, text);
				break;
		}
	}

	private static String createUri(String path) {
		return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).build().toUriString();
	}

}