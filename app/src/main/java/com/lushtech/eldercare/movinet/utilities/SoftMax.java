package com.lushtech.eldercare.movinet.utilities;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class SoftMax {

    private float input;

    private float[] neuronValues;

    public SoftMax(float input, float[] neuronValues) {
        this.input = input;
        this.neuronValues = neuronValues;
    }


    public double get_softmax() {

        DoubleStream ds = IntStream.range(0, neuronValues.length)
                .mapToDouble(i -> neuronValues[i]);

        double total = ds.map(Math::exp).sum();

        return Math.exp(input) / total;
    }
}
