import com.sun.net.httpserver.HttpServer; // 簡単なHTTPサーバーを使うための機能を読み込みます。
import java.net.InetSocketAddress; // サーバーの待ち受け場所とポート番号を指定する機能を読み込みます。
import java.net.URLDecoder; // URL用に変換された文字を元に戻す機能を読み込みます。
import java.nio.charset.StandardCharsets; // 文字コードを指定する機能を読み込みます。
import java.util.ArrayList; // Todoを複数入れるリストを作る機能を読み込みます。
import java.util.List; // Todoのリストを扱う機能を読み込みます。

public class App { // Appという名前のプログラムを定義します。
    static List<Todo> todos = new ArrayList<>(); // ★変更 Todoを保存するリスト（複数のTodoを入れる箱）を用意します。
    static int nextId = 1; // ★変更 次に使うTodoの番号を用意します。

    public static void main(String[] args) throws Exception { // プログラム開始時に実行される場所を定義します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでサーバーを用意します。
        todos.add(new Todo(nextId++, "牛乳を買う")); // ★変更 起動時の1件目のサンプルTodoを追加します。
        Todo egg = new Todo(nextId++, "卵を買う"); // ★変更 起動時の2件目のサンプルTodoを用意します。
        egg.setDone(true); // ★変更 2件目のサンプルTodoを完了済みにします。
        todos.add(egg); // ★変更 2件目のサンプルTodoを追加します。
        server.createContext("/", exchange -> { // ブラウザからトップページへのアクセスを処理します。
            String path = exchange.getRequestURI().getPath(); // ブラウザからアクセスされたパスを取り出します。
            String method = exchange.getRequestMethod(); // GETやPOSTなどの方法を取り出します。
            String message; // ブラウザに返す文字を用意する変数を宣言します。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常はUTF-8のプレーンテキスト（普通の文字）で返します。
            if (path.equals("/add") && method.equals("POST")) { // Todoの追加がPOSTで送られたかどうかを比べます。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 送られてきた中身をUTF-8で受け取ります。
                String value = body.substring(5); // todo=の5文字を除いた値を取り出します。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★変更 URL用に変換されたTodoの題名を日本語に戻します。
                if (!title.isEmpty()) { // ★変更 Todoの題名が空ではないかどうかを比べます。
                    todos.add(new Todo(nextId, title)); // ★変更 Todoを1件作ってリストに追加します。
                    nextId++; // ★変更 次のTodoに使う番号を1つ進めます。
                }
                exchange.getResponseHeaders().set("Location", "/"); // 戻り先をトップページに指定します。
                exchange.sendResponseHeaders(303, -1); // トップページへ戻す応答を送ります。
                exchange.close(); // 通信を閉じます。
                return; // この分岐を終了します。
            } else if (path.equals("/")) { // パスがトップページかどうかを比べます。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>"; // Todo追加フォームと一覧のHTML（Webページの記述）を始めます。
                for (Todo todo : todos) { // ★変更 Todoを1件ずつ取り出します。
                    String mark = ""; // ★変更 完了印を入れる文字を用意します。
                    if (todo.isDone()) { // ★変更 Todoが完了済みかどうかを比べます。
                        mark = " ✔"; // ★変更 完了済みのTodoに印を付けます。
                    }
                    html += "<li>" + todo.getTitle() + mark + "</li>"; // ★変更 Todoの題名と完了印をリスト項目として追加します。
                }
                html += "</ul>"; // Todo一覧のHTMLを閉じます。
                message = html; // 組み立てたフォームと一覧を返す中身にします。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } else { // どのパスにも当てはまらない場合です。
                message = "ページが見つかりません"; // 見つからないパスに返す文字を入れます。
            } // パスによる振り分けを終了します。
            byte[] body = message.getBytes("UTF-8"); // 返す文字をUTF-8のバイト列に変換します。
            exchange.sendResponseHeaders(200, body.length); // 成功を表す番号と返すデータの長さを送ります。
            exchange.getResponseBody().write(body); // ブラウザへデータを送ります。
            exchange.getResponseBody().close(); // データを送り終えたので通信を閉じます。
        }); // トップページの処理を登録します。
        server.start(); // サーバーの待ち受けを開始します。
        System.out.println("サーバー起動: [http://localhost:8080](http://localhost:8080) （止めるときは Ctrl+C）"); // 起動メッセージをターミナルに表示します。
    } // mainメソッドを終了します。
} // Appクラスを終了します。

class Todo { // ★変更 Todoのデータ（番号・題名・完了状態）をまとめるクラスを作ります。
    private final int id; // ★変更 Todoの番号を保存します。
    private final String title; // ★変更 Todoの題名を保存します。
    private boolean done; // ★変更 Todoが完了したかどうかを保存します。

    Todo(int id, String title) { // ★変更 Todoを作るときに番号と題名を受け取ります。
        this.id = id; // ★変更 受け取った番号を保存します。
        this.title = title; // ★変更 受け取った題名を保存します。
        this.done = false; // ★変更 最初は未完了にします。
    }

    int getId() { // ★変更 Todoの番号を読み出します。
        return id; // ★変更 Todoの番号を返します。
    }

    String getTitle() { // ★変更 Todoの題名を読み出します。
        return title; // ★変更 Todoの題名を返します。
    }

    boolean isDone() { // ★変更 Todoの完了状態を読み出します。
        return done; // ★変更 Todoの完了状態を返します。
    }

    void setDone(boolean done) { // ★変更 Todoの完了状態を書き換えます。
        this.done = done; // ★変更 受け取った完了状態を保存します。
    }
}
