package com.prabhstein.pdfanalyser.document;

import java.util.List;
import java.util.stream.Collectors;

final class VectorFormat {

    private VectorFormat() {
    }

    static String toSqlVector(List<Double> embedding) {
        return embedding.stream()
                .map(value -> Double.toString(value == null ? 0.0 : value))
                .collect(Collectors.joining(",", "[", "]"));
    }
}
