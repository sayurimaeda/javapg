import com.sun.net.httpserver.HttpServer; // 簡単なHTTPサーバーを使うための機能を読み込みます。
import java.net.InetSocketAddress; // サーバーの待ち受け場所とポート番号を指定する機能を読み込みます。
import java.net.URLDecoder; // URL用に変換された文字を元に戻す機能を読み込みます。
import java.nio.charset.StandardCharsets; // 文字コードを指定する機能を読み込みます。
import java.nio.file.Files; // ファイルの読み書きをする機能を読み込みます。
import java.nio.file.Path; // ファイルの場所を表す機能を読み込みます。
import java.io.IOException; // ファイル操作で起きるエラーを扱う機能を読み込みます。
import java.util.ArrayList; // Todoを複数入れるリストを作る機能を読み込みます。
import java.util.List; // Todoのリストを扱う機能を読み込みます。

public class App { // Appという名前のプログラムを定義します。
    static List<Todo> todos = new ArrayList<>(); // ★変更 Todoを保存するリスト（複数のTodoを入れる箱）です。
    static int nextId = 1; // ★変更 次に振る番号を1から始めます。

    public static void main(String[] args) throws Exception { // プログラム開始時に実行される場所を定義します。
        load(); // ★追加 起動時にtodos.csvからTodoを読み込みます。

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでサーバーを用意します。
        server.createContext("/", exchange -> { // ブラウザからトップページへのアクセスを処理します。
            String path = exchange.getRequestURI().getPath(); // ブラウザからアクセスされたパスを取り出します。
            String method = exchange.getRequestMethod(); // GETやPOSTなどの方法を取り出します。
            String message; // ブラウザに返す文字を用意する変数を宣言します。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常はUTF-8のプレーンテキスト（普通の文字）で返します。
            if (path.equals("/add") && method.equals("POST")) { // Todoの追加がPOSTで送られたかどうかを比べます。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 送られてきた中身をUTF-8で受け取ります。
                String value = body.substring(5); // todo=の5文字を除いた値を取り出します。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★変更 URL用に変換されたTodoの題名を日本語に戻します。
                if (!title.isEmpty()) { // Todoが空ではないかどうかを比べます。
                    todos.add(new Todo(nextId, title)); // ★変更 Todoを1件作ってリストに追加します。
                    nextId++; // ★変更 次のTodoに使う番号を増やします。
                    save();// ★追加 完了状態の変更をtodos.csvに保存します。
                }
                exchange.getResponseHeaders().set("Location", "/"); // 戻り先をトップページに指定します。
                exchange.sendResponseHeaders(303, -1); // トップページへ戻す応答を送ります。
                exchange.close(); // 通信を閉じます。
                return; // この分岐を終了します。
            } else if (path.equals("/done") && method.equals("GET")) { // ★追加 完了リンクへのアクセスを処理します。
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLのidを含むクエリを受け取ります。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 idが付いているか確認します。
                    try { // ★追加 idを数字に変換します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加 idを数に変えます。
                        for (Todo todo : todos) { // ★追加 Todoを1件ずつ確認します。
                            if (todo.getId() == id) { // ★追加 idが一致するか確認します。
                                todo.setDone(true); // ★追加 Todoを完了済みにします。
                                save(); // ★追加 完了状態の変更をtodos.csvに保存します。
                                break; // ★追加 一致したTodoの確認を終えます。
                            }
                        }
                    } catch (NumberFormatException e) { // ★追加 数字でないidは何もしません。
                    }
                }
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 トップページへ戻します。
                exchange.sendResponseHeaders(303, -1); // ★追加 トップページへ戻す応答を送ります。
                exchange.close(); // ★追加 通信を閉じます。
                return; // ★追加 この分岐を終了します。
            } else if (path.equals("/delete") && method.equals("GET")) { // ★追加 削除リンクへのアクセスを処理します。
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLのidを含むクエリを受け取ります。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 idが付いているか確認します。
                    try { // ★追加 idを数字に変換します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加 idを数に変えます。
                        boolean removed = todos.removeIf(todo -> todo.getId() == id); // ★追加 idが一致するTodoを削除します。
                        if (removed) { // ★追加 Todoが削除されたか確認します。
                            save(); // ★追加 削除後の内容をtodos.csvに保存します。
                        }
                    } catch (NumberFormatException e) { // ★追加 数字でないidは何もしません。
                    }
                }
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 トップページへ戻します。
                exchange.sendResponseHeaders(303, -1); // ★追加 トップページへ戻す応答を送ります。
                exchange.close(); // ★追加 通信を閉じます。
                return; // ★追加 この分岐を終了します。
            } else if (path.equals("/")) { // パスがトップページかどうかを比べます。
                String html = "<!doctype html><html><head><meta charset='UTF-8'><style>body{max-width:600px;margin:2rem auto;padding:0 1rem;font-size:1rem}</style></head><body><h1>今日のおつとめじゃ</h1><form method='post' action='/add'><input name='todo'><button>追加</button></form>"; // ★追加
                                                                                                                                                                                                                                                                                    // ページの見出しと最小限の幅・余白・文字サイズを設定します。
                if (todos.isEmpty()) { // ★追加 Todoが0件か確認します。
                    html += "<p>今おつとめは無いようじゃの</p>"; // ★追加 Todoがないときの案内を表示します。
                } else { // ★追加 Todoがある場合です。
                    html += "<ul>"; // ★追加 Todo一覧を始めます。
                    for (Todo todo : todos) { // ★変更 Todoを1件ずつ取り出します。
                        String mark = ""; // ★変更 完了済みの印を入れる文字を用意します。
                        if (todo.isDone()) { // ★変更 Todoが完了済みかどうかを確認します。
                            mark = " ✔"; // ★変更 完了済みの印を入れます。
                        }
                        html += "<li>" + todo.getTitle() + mark + " <a href='/done?id=" + todo.getId()
                                + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加
                                                                                                    // title、完了リンク、削除リンクを一覧に追加します。
                    }
                    html += "</ul>"; // ★追加 Todo一覧のHTMLを閉じます。
                }
                html += "</body></html>"; // ★追加 ページのHTMLを閉じます。
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

    static void save() { // ★追加 Todo全件をtodos.csvへ保存するメソッドです。
        List<String> lines = new ArrayList<>(); // ★追加 CSV（カンマ区切りのテキスト）の各行を入れます。
        for (Todo todo : todos) { // ★追加 Todoを1件ずつ保存用の行にします。
            lines.add(todo.getId() + "," + (todo.isDone() ? "1" : "0") + "," + todo.getTitle()); // ★追加 id、完了状態、題名を書きます。
        }
        try { // ★追加 ファイルへ書き込みます。
            Files.write(Path.of("todos.csv"), lines, StandardCharsets.UTF_8); // ★追加 UTF-8で全件を書き出します。
        } catch (IOException e) { // ★追加 ファイルの書き込みエラーを受け止めます。
            System.err.println("todos.csvを保存できませんでした: " + e.getMessage()); // ★追加 エラー内容を表示します。
        }
    }

    static void load() { // ★追加 todos.csvからTodo全件を読み込むメソッドです。
        Path path = Path.of("todos.csv"); // ★追加 読み込むファイルの場所を指定します。
        if (!Files.exists(path)) { // ★追加 ファイルがあるか確認します。
            return; // ★追加 無ければTodoなしで起動します。
        }
        try { // ★追加 ファイルを読み込みます。
            int maxId = 0; // ★追加 読み込んだ中で最大の番号を記録します。
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) { // ★追加 UTF-8で各行を読みます。
                String[] fields = line.split(",", 3); // ★追加 id、完了状態、題名の3つに分けます。
                if (fields.length == 3) { // ★追加 3項目そろっているか確認します。
                    int id = Integer.parseInt(fields[0]); // ★追加 idを数に変えます。
                    Todo todo = new Todo(id, fields[2]); // ★追加 読み込んだidと題名でTodoを作ります。
                    todo.setDone(fields[1].equals("1")); // ★追加 完了状態を読み込みます。
                    todos.add(todo); // ★追加 Todoをリストに戻します。
                    if (id > maxId) { // ★追加 最大idか確認します。
                        maxId = id; // ★追加 最大idを更新します。
                    }
                }
            }
            nextId = maxId + 1; // ★追加 次のidを最大idの次にします。
        } catch (IOException | NumberFormatException e) { // ★追加 読み込みやid変換のエラーを受け止めます。
            System.err.println("todos.csvを読み込めませんでした: " + e.getMessage()); // ★追加 エラー内容を表示します。
        }
    }
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
