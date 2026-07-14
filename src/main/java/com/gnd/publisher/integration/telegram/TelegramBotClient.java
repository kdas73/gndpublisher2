package com.gnd.publisher.integration.telegram;

import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.dto.telegram.TelegramMessageDto;
import com.gnd.publisher.dto.telegram.TelegramSendResult;

public interface TelegramBotClient {

    TelegramSendResult sendMessage(TelegramChannel channel, TelegramMessageDto message);
}
