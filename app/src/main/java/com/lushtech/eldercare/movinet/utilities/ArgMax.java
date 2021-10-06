package com.lushtech.eldercare.movinet.utilities;

public class ArgMax {

    private float[] params;

    public ArgMax(float[] params) {
        this.params = params;
    }

    public Result getResult() {
        int maxIndex = 0;
        for (int i=0; i<params.length; i++) {
            if (params[maxIndex] < params[i]) {
                maxIndex = i;
            }
        }

        return new Result(maxIndex, params[maxIndex]);
    }

    public class Result {
        private int index;
        private float maxValue;

        public Result(int index, float maxValue) {
            this.index = index;
            this.maxValue = maxValue;
        }

        public int getIndex() {
            return index;
        }

        public float getMaxValue() {
            return maxValue;
        }
    }
}

