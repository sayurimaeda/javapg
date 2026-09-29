class Items {
    public static void main(String[] args) {
        String[] todos = {"牛乳を買う", "卵を買う", "パンを買う", "掃除をする"};
        for (int i = 0; i < todos.length; i++) {
            System.out.println("<li>" + todos[i] + "</li>");
        }
    }
}
