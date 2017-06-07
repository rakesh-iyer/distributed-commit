import java.io.*;

class Message implements Serializable {
    String type;
    String data;
    final static int maxSize = 1000;

    String getType() {
        return type;
    }

    String getData() {
        return data;
    }

    void setType(String type) {
        this.type = type;
    }

    void setData(String data) {
        this.data = data;
    }

    static int getMaxSize() {
        return maxSize;
    }

    static boolean isValid() {
        return true;
    }

    public String toString() {
        return "type:" + type + " data:" + data;
    }
}
