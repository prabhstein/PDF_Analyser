package com.prabhstein.pdfanalyser.chat;

import java.util.List;

public record AnswerResult(String answer, List<Citation> citations, boolean notAnswerable) {
}
