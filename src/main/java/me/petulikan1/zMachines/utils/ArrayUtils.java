package me.petulikan1.zMachines.utils;

import java.lang.reflect.Array;

@SuppressWarnings("unchecked")
public class ArrayUtils {
    public static <T> T[] sumArrays(T[] first, T... second) {
        if (first == null || first.length == 0) {
            return second;
        }
        if (second == null || second.length == 0) {
            return first;
        }

        T[] result = newInstance(first.getClass().getComponentType(), first.length + second.length);

        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static <T> T[] sumArrays(T[]... arrays) {
        if (arrays == null) {
            throw new NullPointerException("Array cannot be null");
        }
        if (arrays.length == 0) {
            return newInstance(arrays.getClass().getComponentType().getComponentType(), 0);
        }

        int totalSize = 0;
        for (T[] array2 : arrays) {
            totalSize += array2.length;
        }

        T[] result = newInstance(arrays.getClass().getComponentType().getComponentType(), totalSize);

        int endPos = 0;
        for (T[] array : arrays) {
            System.arraycopy(array, 0, result, endPos, array.length);
            endPos += array.length;
        }
        return result;
    }

    public static <T> T[] newInstance(Class<?> clazz, int size) {
        return (T[]) (clazz == Object[].class ? new Object[size] : Array.newInstance(clazz, size));
    }

    public static Object newSafeInstance(Class<?> clazz, int size) {
        return clazz == Object[].class ? new Object[size] : Array.newInstance(clazz, size);
    }

    public static void move(Object array, int fromPos, int toPos) {
        if (fromPos == toPos) {
            return;
        }

        Object val = Array.get(array, fromPos);


        System.arraycopy(array, 0, array, 0, fromPos);


        System.arraycopy(array, fromPos + (fromPos < toPos ? 1 : 0), array, fromPos, Array.getLength(array) - 1 - fromPos);


        System.arraycopy(array, toPos, array, toPos + 1, Array.getLength(array) - 1 - toPos);


        Array.set(array, toPos, val);
    }

    public static void move(Object array, int from, int to, int length) {
        if (from < 0 || from > to) {
            throw new IndexOutOfBoundsException("Array length is " + length + ", moving from position " + from + " to " + to);
        }
        System.arraycopy(array, from, array, to, length - from);
    }

    public static void insert(Object array, int position, Object value, int length) {
        if (position < 0 || position >= length) {
            throw new IndexOutOfBoundsException("Array length is " + (length - 1) + ", insert position is " + position);
        }
        System.arraycopy(array, position, array, position + 1, length - position - 1);
        Array.set(array, position, value);
    }


    public static Object insert(Object array, int position, Object value) {
        int length = Array.getLength(array) + 1;
        if (position < 0 || position >= length) {
            throw new IndexOutOfBoundsException("Array length is " + (length - 1) + ", insert position is " + position);
        }
        Object newInstance = array.getClass().getComponentType() == Object[].class ? new Object[length] : Array.newInstance(array.getClass().getComponentType(), length);
        System.arraycopy(array, 0, newInstance, 0, position);
        System.arraycopy(array, position, newInstance, position + 1, length - position - 1);
        Array.set(newInstance, position, value);
        return newInstance;
    }
}
