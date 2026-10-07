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
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER, due_date TEXT)"); // ★
            // 要件の列定義でtodos表を作成します。
        }

        ensureDueDateColumn();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String title = formValue(body, "todo");
                String dueDate = formValue(body, "dueDate");
                if (!title.isEmpty()) {
                    try (PreparedStatement statement = connection
                            .prepareStatement("INSERT INTO todos (title, done, due_date) VALUES (?, ?, ?)")) { // ★
                                                                                                               // 新規TodoをINSERTします。
                        statement.setString(1, title);
                        statement.setInt(2, 0);
                        statement.setString(3, dueDate);
                        statement.executeUpdate(); // ★ PreparedStatementで追加を実行します。
                    } catch (SQLException e) {
                        throw new IOException(e);
                    }
                }
                redirect(exchange);
                return;
            }
            if (path.equals("/due") && method.equals("POST")) { // 締め切り日変更フォームから指定されたTodoを更新します。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // フォームの送信内容をUTF-8で読みます。
                String filter = formValue(body, "filter"); // 更新後も現在の絞り込みを保ちます。
                if (!filter.equals("open") && !filter.equals("done")) filter = "all"; // 不正な絞り込み値は全部表示に戻します。
                try {
                    int id = Integer.parseInt(formValue(body, "id")); // idで更新対象のTodoを特定します。
                    String dueDate = formValue(body, "dueDate"); // 新しい締め切り日を読みます。
                    try (PreparedStatement statement = connection.prepareStatement("UPDATE todos SET due_date = ? WHERE id = ?")) { // 指定Todoの日付だけを更新します。
                        statement.setString(1, dueDate); // 未入力なら締め切り日を空にします。
                        statement.setInt(2, id); // 更新対象のidを指定します。
                        statement.executeUpdate(); // SQLiteに日付変更を反映します。
                    }
                } catch (NumberFormatException ignored) { // 不正なidでは更新しません。
                } catch (SQLException e) {
                    throw new IOException(e);
                }
                exchange.getResponseHeaders().set("Location", "/?filter=" + filter); // 同じ絞り込み条件の一覧へ戻します。
                exchange.sendResponseHeaders(303, -1); // 変更後の一覧を再読み込みさせます。
                exchange.close(); // 通信を閉じます。
                return; // 更新処理を終えます。
            }
            if ((path.equals("/done") || path.equals("/delete")) && method.equals("GET")) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    try {
                        int id = Integer.parseInt(query.substring(3));
                        if (path.equals("/done")) {
                            try (PreparedStatement statement = connection
                                    .prepareStatement("UPDATE todos SET done = ? WHERE id = ?")) { // ★ 完了状態をUPDATEします。
                                statement.setInt(1, 1);
                                statement.setInt(2, id);
                                statement.executeUpdate(); // ★ PreparedStatementで更新を実行します。
                            }
                        } else {
                            try (PreparedStatement statement = connection
                                    .prepareStatement("DELETE FROM todos WHERE id = ?")) { // ★ TodoをDELETEします。
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

            StringBuilder html = new StringBuilder(
                    "<!doctype html><html><head><meta charset='UTF-8'><style>\n"
                            + "*{box-sizing:border-box}body{max-width:680px;margin:2.5rem auto;padding:0 1.25rem;font-size:1rem;line-height:1.6;color:#243044;background:#f5f7fb;font-family:system-ui,-apple-system,'Segoe UI',sans-serif}h1{margin:0 0 1.5rem;color:#243044;font-size:1.8rem}form{margin:0}body>form:first-of-type{display:flex;flex-wrap:wrap;align-items:center;gap:.65rem;margin-bottom:1.25rem;padding:1rem;background:#fff;border:1px solid #e1e7f0;border-radius:12px}input,select,button{min-height:42px;padding:.55rem .75rem;border:1px solid #cbd5e1;border-radius:8px;font:inherit}input[name=todo]{flex:1;min-width:180px}label{display:inline-flex;align-items:center;gap:.5rem}button{border:0;background:#3568c8;color:#fff;font-weight:600;cursor:pointer}button:hover{background:#2855a8}select{background:#fff}ul{display:grid;gap:1rem;margin:1.25rem 0;padding:0;list-style:none}li.todo-card{padding:1.15rem 1.25rem;background:#fff;border:1px solid #e1e7f0;border-radius:12px;box-shadow:0 3px 12px rgba(31,48,74,.06)}.todo-title{font-size:1.25rem;font-weight:700;overflow-wrap:anywhere}.todo-due{margin-top:.25rem;color:#5b6678}.due-form{display:flex;flex-wrap:wrap;align-items:center;gap:.6rem;margin-top:.75rem}.todo-actions{display:flex;gap:.65rem;margin-top:.9rem;padding-top:.75rem;border-top:1px solid #edf0f5}.todo-actions a{padding:.35rem .75rem;border-radius:7px;background:#edf3ff;color:#2855a8;text-decoration:none;font-weight:600}.todo-actions a:last-child{background:#fff0f0;color:#b42318}@media(max-width:520px){body{margin:1.25rem auto;padding:0 .85rem}body>form:first-of-type{align-items:stretch}body>form:first-of-type input[name=todo]{flex-basis:100%}.due-form input[type=date]{flex:1;min-width:150px}}</style></head><body><h1>今日のおつとめじゃ</h1><form method='post' action='/add'><input name='todo'><label>締め切り日 <input type='date' name='dueDate'></label><button>追加</button></form>");
            String filter = "all"; // ★初期状態では全部を表示します。
            String query = exchange.getRequestURI().getQuery(); // ★選択された表示条件をURLから読み取ります。
            if ("filter=open".equals(query)) filter = "open"; // ★未完了だけを選んだ状態にします。
            else if ("filter=done".equals(query)) filter = "done"; // ★完了だけを選んだ状態にします。
            html.append("<form method='get' action='/'><label>表示: <select name='filter' onchange='this.form.submit()'>") // ★一覧の絞り込み選択欄を追加します。
                    .append("<option value='all'").append(filter.equals("all") ? " selected" : "").append(">全部</option>") // ★全部表示を選択肢にします。
                    .append("<option value='open'").append(filter.equals("open") ? " selected" : "").append(">未完了だけ</option>") // ★未完了のみを選択肢にします。
                    .append("<option value='done'").append(filter.equals("done") ? " selected" : "").append(">完了だけ</option>") // ★完了のみを選択肢にします。
                    .append("</select></label></form>"); // ★選択欄を閉じます。
            try {
                List<Todo> todos = loadTodos(); // ★ 一覧をSQLiteのSELECT結果から取得します。
                List<Todo> visibleTodos = new ArrayList<>(); // ★表示対象だけを入れる一時リストを作ります。
                for (Todo todo : todos) { // ★DBから読んだTodoを変更せずに絞り込みます。
                    if (filter.equals("all") || (filter.equals("open") && !todo.done) || (filter.equals("done") && todo.done)) { // ★選択状態に合うTodoだけを表示対象にします。
                        visibleTodos.add(todo); // ★条件に合うTodoを表示用リストに追加します。
                    }
                }
                if (visibleTodos.isEmpty()) {
                    html.append("<p>今はおつとめは、無いようじゃな</p>");
                } else {
                    html.append("<ul class='todo-list'>");
                    for (Todo todo : visibleTodos) {
                        String mark = todo.done ? " ✓" : "";
                        html.append("<li class='todo-card'><div class='todo-title'>").append(escapeHtml(todo.title)).append(mark)
                                .append("</div><div class='todo-due'>締め切り日: ")
                                .append(todo.dueDate == null || todo.dueDate.isEmpty() ? "未設定"
                                        : escapeHtml(todo.dueDate))
                                .append("</div>")
                                .append(" <form method='post' action='/due' class='due-form'>") // Todoごとの締め切り日編集フォームです。
                                .append("<input type='hidden' name='id' value='").append(todo.id).append("'>") // 編集対象のTodo idを送信します。
                                .append("<input type='hidden' name='filter' value='").append(filter).append("'>") // 現在の絞り込み状態を送信します。
                                .append("<input type='date' name='dueDate' value='").append(todo.dueDate == null ? "" : escapeHtml(todo.dueDate)).append("'>") // 現在の日付をdate欄に表示します。
                                .append("<button>締め切り日を変更</button></form>") // 入力した日付を保存するボタンです。
                                .append("<div class='todo-actions'><a href='/done?id=").append(todo.id).append("'>完了</a>")
                                .append(" <a href='/delete?id=").append(todo.id).append("'>削除</a></div></li>");
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
        server.createContext("/api/todos", exchange -> { // ★ Todo一覧JSON用の入口を追加します。
            if (!exchange.getRequestMethod().equals("GET")
                    || !exchange.getRequestURI().getPath().equals("/api/todos")) { // ★ GET /api/todos以外を拒否します。
                exchange.sendResponseHeaders(404, -1); // ★ 対象外のリクエストには404を返します。
                exchange.close(); // ★ 対象外の通信を閉じます。
                return; // ★ JSON応答処理を終了します。
            }
            try { // ★ DB読み込み時のエラーを処理します。
                byte[] response = todosToJson(loadTodos()).getBytes(StandardCharsets.UTF_8); // ★
                                                                                             // 全TodoをJSONにしてUTF-8へ変換します。
                exchange.getResponseHeaders().set("Content-Type", "application/json"); // ★ charsetを付けずにJSON形式を指定します。
                exchange.sendResponseHeaders(200, response.length); // ★ 成功と応答サイズを返します。
                exchange.getResponseBody().write(response); // ★ JSONデータを返します。
            } catch (SQLException e) { // ★ SQLiteの読み込みエラーを受け取ります。
                throw new IOException(e); // ★ HTTPハンドラーで扱える入出力エラーにします。
            } finally { // ★ 成功・失敗のどちらでも通信を閉じます。
                exchange.close(); // ★ HTTP通信を終了します。
            }
        }); // ★ JSON用の入口を登録します。
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 (停止は Ctrl+C)");
    }

    private static String formValue(String body, String name) {
        for (String part : body.split("&")) {
            String[] pair = part.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            if (key.equals(name)) {
                return pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            }
        }
        return "";
    }

    private static void ensureDueDateColumn() throws SQLException {
        boolean exists = false;
        try (Statement statement = connection.createStatement();
                ResultSet columns = statement.executeQuery("PRAGMA table_info(todos)")) {
            while (columns.next()) {
                if ("due_date".equals(columns.getString("name")))
                    exists = true;
            }
        }
        if (!exists) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE todos ADD COLUMN due_date TEXT");
            }
        }
    }

    private static List<Todo> loadTodos() throws SQLException { // ★ CSV読み込みに代わりSQLiteから全件をSELECTします。
        List<Todo> todos = new ArrayList<>();
        try (PreparedStatement statement = connection
                .prepareStatement("SELECT id, title, done, due_date FROM todos ORDER BY id"); // ★
                // 一覧表示用のSELECTです。
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                todos.add(new Todo(result.getInt("id"), result.getString("title"), result.getInt("done") != 0,
                        result.getString("due_date"))); // ★
                // DB行をTodoに変換します。
            }
        }
        return todos;
    }

    private static String todosToJson(List<Todo> todos) { // ★ Todo一覧を指定形式のJSON配列にします。
        StringBuilder json = new StringBuilder("["); // ★ JSON配列の開始記号を追加します。
        for (int i = 0; i < todos.size(); i++) { // ★ Todoを先頭から順にJSONへ変換します。
            Todo todo = todos.get(i); // ★ 今変換するTodoを取り出します。
            if (i > 0)
                json.append(','); // ★ 2件目以降の前に区切りカンマを追加します。
            json.append("{\"title\":\"").append(escapeJson(todo.title)).append("\",\"done\":").append(todo.done)
                    .append('}'); // ★ タイトルと完了状態をJSONオブジェクトにします。
        }
        return json.append(']').toString(); // ★ 配列を閉じてJSON文字列を返します。
    }

    private static String escapeJson(String value) { // ★ JSON文字列内で特別な意味を持つ文字をエスケープします。
        StringBuilder escaped = new StringBuilder(); // ★ エスケープ後の文字列を作ります。
        for (int i = 0; i < value.length(); i++) { // ★ タイトルを1文字ずつ確認します。
            char c = value.charAt(i); // ★ 現在の文字を取り出します。
            switch (c) { // ★ JSONで特別扱いする文字を判定します。
                case '"':
                    escaped.append("\\\"");
                    break; // ★ 二重引用符をエスケープします。
                case '\\':
                    escaped.append("\\\\");
                    break; // ★ バックスラッシュをエスケープします。
                case '\n':
                    escaped.append("\\n");
                    break; // ★ 改行をエスケープします。
                case '\r':
                    escaped.append("\\r");
                    break; // ★ 復帰文字をエスケープします。
                case '\t':
                    escaped.append("\\t");
                    break; // ★ タブをエスケープします。
                case '\b':
                    escaped.append("\\b");
                    break; // ★ バックスペースをエスケープします。
                case '\f':
                    escaped.append("\\f");
                    break; // ★ フォームフィードをエスケープします。
                default: // ★ それ以外の文字を確認します。
                    if (c < 0x20)
                        escaped.append(String.format("\\u%04x", (int) c)); // ★ その他の制御文字をUnicode表記にします。
                    else
                        escaped.append(c); // ★ 通常の文字はそのまま追加します。
            }
        }
        return escaped.toString(); // ★ エスケープしたタイトルを返します。
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
        final String dueDate;

        Todo(int id, String title, boolean done, String dueDate) {
            this.id = id;
            this.title = title;
            this.done = done;
            this.dueDate = dueDate;
        }
    }
}
