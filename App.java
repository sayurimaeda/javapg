import com.sun.net.httpserver.HttpServer; // 簡単なHTTPサーバーを使うための機能を読み込みます。
import java.net.InetSocketAddress; // サーバーの待ち受け場所とポート番号を指定する機能を読み込みます。
import java.net.URLDecoder; // URL用に変換された文字を元に戻す機能を読み込みます。

public class App { // Appという名前のプログラムを定義します。
    public static void main(String[] args) throws Exception { // プログラム開始時に実行される場所を定義します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでサーバーを用意します。
        server.createContext("/", exchange -> { // ブラウザからトップページへのアクセスを処理します。
            String path = exchange.getRequestURI().getPath(); // ブラウザからアクセスされたパスを取り出します。
            String message; // ブラウザに返す文字を用意する変数を宣言します。
            if (path.equals("/hello")) { // パスが/helloかどうかを比べます。
                String query = exchange.getRequestURI().getRawQuery(); // URLのクエリを取り出します。
                System.out.println("query = " + query);
                String name = query.substring(5); // name=の後ろを切り出します。
                name = URLDecoder.decode(name, "UTF-8"); // URL用に変換された名前を日本語に戻します。
                message = "こんにちは、" + name + "さん！"; // 名前を応答に混ぜます。
            } else if (path.equals("/bye")) { // パスが/byeかどうかを比べます。
                message = "ごめんあそばせ！"; // /byeに返す文字を入れます。
            } else if (path.equals("/menu")) { // パスが/menuかどうかを比べます。
                message = "紅茶はアールグレーです"; // /menuに返す文字を入れます。
            } else { // どのパスにも当てはまらない場合です。
                message = "ページが見つかりません"; // 見つからないパスに返す文字を入れます。
            } // パスによる振り分けを終了します。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 返す文字がUTF-8であることを伝えます。
            byte[] body = message.getBytes("UTF-8"); // 返す文字をUTF-8のバイト列に変換します。
            exchange.sendResponseHeaders(200, body.length); // 成功を表す番号と返すデータの長さを送ります。
            exchange.getResponseBody().write(body); // ブラウザへデータを送ります。
            exchange.getResponseBody().close(); // データを送り終えたので通信を閉じます。
        }); // トップページの処理を登録します。
        server.start(); // サーバーの待ち受けを開始します。
        System.out.println("サーバー起動: [http://localhost:8080](http://localhost:8080) （止めるときは Ctrl+C）"); // 起動メッセージをターミナルに表示します。
    } // mainメソッドを終了します。
} // Appクラスを終了します。
