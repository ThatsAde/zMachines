package me.petulikan1.zMachines.utils;


public class Validator {

    public static void validate(boolean question, String error) {
        if (question) {
            throw new RuntimeException(error);
        }
    }

    public static void validate2(boolean question, String error) {
        if (!question) {
            throw new RuntimeException(error);
        }
    }

    public static void notNull(Object object, String error) {
        if (object == null) {
            throw new RuntimeException(error);
        }
    }

}
