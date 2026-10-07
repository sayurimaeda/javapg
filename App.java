import com.sun.net.httpserver.HttpServer; // HTTPサーバーを使います。
import java.io.IOException; // HTTP通信の入出力エラーを扱います。
import java.net.InetSocketAddress; // 待ち受けアドレスを指定します。
import java.net.URLDecoder; // フォームのURLエンコードを戻します。
import java.nio.charset.StandardCharsets; // UTF-8を指定します。
import java.sql.Connection; // SQLiteへの接続を保持します。
import java.sql.DriverManager; // SQLiteへ接続します。
import java.sql.PreparedStatement; // SQLを安全に実行します。
import java.sql.ResultSet; // SELECT結果を読み取ります。
import java.sql.SQLException; // データベースエラーを扱います。
import java.sql.Statement; // テーブル作成SQLを実行します。
import java.util.ArrayList; // SELECT結果を一覧にします。
import java.util.List; // Todo一覧の型に使います。

public class App {
    private static Connection connection; // ★ CSVの保存処理に代わり、SQLite接続を共有します。

    public static void main(String[] args) throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite:todos.db"); // ★ todos.dbへ接続します。
        try (Statement statement = connection.createStatement()) { // ★ テーブルを用意するSQL文を作ります。
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER)"); // ★ 要件の列定義でtodos表を作成します。
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String value = body.startsWith("todo=") ? body.substring(5) : "";
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8);
                if (!title.isEmpty()) {
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO todos (title, done) VALUES (?, ?)"); // ★ 新規TodoをINSERTします。
                         ) {
                        statement.setString(1, title);
                        statement.setInt(2, 0);
                        statement.executeUpdate(); // ★ PreparedStatementで追加を実行します。
                    } catch (SQLException e) {
                        throw new IOException(e);
                    }
                }
                redirect(exchange);
                return;
            }
            if ((path.equals("/done") || path.equals("/delete")) && method.equals("GET")) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    try {
                        int id = Integer.parseInt(query.substring(3));
                        if (path.equals("/done")) {
                            try (PreparedStatement statement = connection.prepareStatement("UPDATE todos SET done = ? WHERE id = ?")) { // ★ 完了状態をUPDATEします。
                                statement.setInt(1, 1);
                                statement.setInt(2, id);
                                statement.executeUpdate(); // ★ PreparedStatementで更新を実行します。
                            }
                        } else {
                            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM todos WHERE id = ?")) { // ★ TodoをDELETEします。
                                statement.setInt(1, id);
                                statement.executeUpdate(); // ★ PreparedStatementで削除を実行します。
                            }
                        }
                    } catch (NumberFormatException ignored) {
                        // 不正なIDは何も変更しません。
                    } catch (SQLException e) {
                        throw new IOException(e);
                    }
                }
                redirect(exchange);
                return;
            }
            if (!path.equals("/")) {
                byte[] notFound = "ページが見つかりません".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(404, notFound.length);
                exchange.getResponseBody().write(notFound);
                exchange.close();
                return;
            }

            StringBuilder html = new StringBuilder("<!doctype html><html><head><meta charset='UTF-8'><style>body{max-width:600px;margin:2rem auto;padding:0 1rem;font-size:1rem}</style></head><body><h1>今日のTodo</h1><form method='post' action='/add'><input name='todo'><button>追加</button></form>");
            try {
                List<Todo> todos = loadTodos(); // ★ 一覧をSQLiteのSELECT結果から取得します。
                if (todos.isEmpty()) {
                    html.append("<p>Todoはありません。</p>");
                } else {
                    html.append("<ul>");
                    for (Todo todo : todos) {
                        String mark = todo.done ? " ✓" : "";
                        html.append("<li>").append(escapeHtml(todo.title)).append(mark)
                                .append(" <a href='/done?id=").append(todo.id).append("'>完了</a>")
                                .append(" <a href='/delete?id=").append(todo.id).append("'>削除</a></li>");
                    }
                    html.append("</ul>");
                }
            } catch (SQLException e) {
                throw new IOException(e);
            }
            html.append("</body></html>");
            byte[] response = html.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 (停止は Ctrl+C)");
    }

    private static List<Todo> loadTodos() throws SQLException { // ★ CSV読み込みに代わりSQLiteから全件をSELECTします。
        List<Todo> todos = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT id, title, done FROM todos ORDER BY id"); // ★ 一覧表示用のSELECTです。
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                todos.add(new Todo(result.getInt("id"), result.getString("title"), result.getInt("done") != 0)); // ★ DB行をTodoに変換します。
            }
        }
        return todos;
    }

    private static void redirect(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static class Todo {
        final int id;
        final String title;
        final boolean done;

        Todo(int id, String title, boolean done) {
            this.id = id;
            this.title = title;
            this.done = done;
        }
    }
}
