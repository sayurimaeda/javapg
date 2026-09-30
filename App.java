import com.sun.net.httpserver.HttpServer; // 簡単なHTTPサーバーを使うための機能を読み込みます。
import java.net.InetSocketAddress; // サーバーの待ち受け場所とポート番号を指定する機能を読み込みます。
import java.net.URLDecoder; // URL用に変換された文字を元に戻す機能を読み込みます。
import java.util.ArrayList; // Todoを複数入れるリストを作る機能を読み込みます。
import java.util.List; // Todoのリストを扱う機能を読み込みます。

public class App { // Appという名前のプログラムを定義します。
    public static void main(String[] args) throws Exception { // プログラム開始時に実行される場所を定義します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでサーバーを用意します。
        server.createContext("/", exchange -> { // ブラウザからトップページへのアクセスを処理します。
            String path = exchange.getRequestURI().getPath(); // ブラウザからアクセスされたパスを取り出します。
            String message; // ブラウザに返す文字を用意する変数を宣言します。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常はUTF-8のプレーンテキスト（普通の文字）で返します。
            if (path.equals("/hello")) { // パスが/helloかどうかを比べます。
                String query = exchange.getRequestURI().getRawQuery(); // URLのクエリを取り出します。
                System.out.println("query = " + query);
                String name = query.substring(5); // name=の後ろを切り出します。
                name = URLDecoder.decode(name, "UTF-8"); // URL用に変換された名前を日本語に戻します。
                message = "こんにちは、" + name + "さん！"; // 名前を応答に混ぜます。
            } else if (path.equals("/todos")) { // /todosにアクセスされたかどうかを比べます。
                List<String> todos = new ArrayList<>(); // Todoを入れるリスト（複数の文字列を入れる箱）を作ります。
                todos.add("牛乳を買う"); // Todoを1件追加します。
                todos.add("卵を買う"); // Todoを1件追加します。
                todos.add("パンを買う"); // Todoを1件追加します。
                todos.add("自分の今日のTodo");
                String html = "<ul>"; // Todo一覧のHTML（Webページの記述）を始めます。
                for (String todo : todos) { // Todoを1件ずつ取り出します。
                    html += "<li>" + todo + "</li>"; // Todoをリスト項目として追加します。
                }
                html += "</ul>"; // Todo一覧のHTMLを閉じます。
                message = html; // 組み立てたHTMLを返す中身にします。
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            } else if (path.equals("/bye")) { // パスが/byeかどうかを比べます。
                message = "ごめんあそばせ！"; // /byeに返す文字を入れます。
            } else if (path.equals("/menu")) { // パスが/menuかどうかを比べます。
                message = "紅茶はアールグレーです"; // /menuに返す文字を入れます。
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
