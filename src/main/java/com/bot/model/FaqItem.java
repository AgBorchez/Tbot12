package com.bot.model;

import java.util.List;

public record FaqItem(List<String> keywords, String answer) {}