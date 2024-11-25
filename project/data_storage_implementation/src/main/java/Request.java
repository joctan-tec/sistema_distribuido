class Request {
    final String ip;
    final String operation;
    final String key;
    final String value;

    public Request(String ip, String operation, String key, String value) {
        this.ip = ip;
        this.operation = operation;
        this.key = key;
        this.value = value;
    }
}