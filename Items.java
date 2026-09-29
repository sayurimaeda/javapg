class Items { // Itemsという名前のクラス（コードをまとめる枠）です。
    public static void main(String[] args) { // mainはプログラム実行時の入口です。
        String[] todos = {"牛乳を買う", "卵を買う", "パンを買う"}; // todosはTodoを並べて保存する配列（データの並び）です。
        for (int i = 0; i < todos.length; i++) { // iを0から始め、配列の個数まで1つずつ進めます。
            System.out.println("<li>" + todos[i] + "</li>"); // i番目のTodoを<li>と</li>で囲んで1行出力します。
        } // for文（繰り返し）の範囲はここまでです。
    } // mainメソッド（実行の入口）の範囲はここまでです。
} // Itemsクラスの範囲はここまでです。
