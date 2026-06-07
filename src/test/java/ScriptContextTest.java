import com.gitee.planners.module.script.ScriptContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 复现 ScriptContext (ThreadLocal) 在异步线程中丢失的 bug
 *
 * 问题: ImmutableSkill.execute() 使用 CompletableFuture.supplyAsync(task)
 *       在 task 中设置了 ScriptContext.setCurrent(vars)
 *       但 GraalJS eval 执行脚本时，ScriptContext.getSender() 返回 null
 */
public class ScriptContextTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== ScriptContext ThreadLocal 行为测试 ===\n");

        // 测试1: 主线程设置，主线程读取
        testMainThread();

        // 测试2: 主线程设置，异步线程读取
        testMainThreadSetAsyncRead();

        // 测试3: 异步线程设置，异步线程读取 (模拟 ImmutableSkill.execute 的场景)
        testAsyncSetAsyncRead();

        // 测试4: 异步线程设置，异步线程读取 (使用 supplyAsync)
        testSupplyAsyncSetRead();
    }

    static void testMainThread() {
        System.out.println("[测试1] 主线程设置，主线程读取:");
        Map<String, Object> vars = createVars("Player1");
        ScriptContext.setCurrent(vars);

        Object sender = ScriptContext.getSender();
        System.out.println("  sender: " + sender);
        System.out.println("  状态: " + ("Player1".equals(sender) ? "✓ 通过" : "✗ 失败"));
        System.out.println();

        ScriptContext.clear();
    }

    static void testMainThreadSetAsyncRead() throws Exception {
        System.out.println("[测试2] 主线程设置，异步线程读取:");
        Map<String, Object> vars = createVars("Player2");
        ScriptContext.setCurrent(vars);

        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("  异步线程: " + Thread.currentThread().getName());
            return ScriptContext.getSender();
        });

        Object sender = future.get();
        System.out.println("  sender: " + sender);
        System.out.println("  预期: null (ThreadLocal 不跨线程传递)");
        System.out.println("  状态: " + (sender == null ? "✓ 符合预期" : "✗ 不符合预期"));
        System.out.println();

        ScriptContext.clear();
    }

    static void testAsyncSetAsyncRead() throws Exception {
        System.out.println("[测试3] 异步线程设置，异步线程读取 (同一线程):");
        Map<String, Object> vars = createVars("Player3");

        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("  异步线程: " + Thread.currentThread().getName());
            ScriptContext.setCurrent(vars);
            Object sender = ScriptContext.getSender();
            ScriptContext.clear();
            return sender;
        });

        Object sender = future.get();
        System.out.println("  sender: " + sender);
        System.out.println("  预期: Player3");
        System.out.println("  状态: " + ("Player3".equals(sender) ? "✓ 通过" : "✗ 失败"));
        System.out.println();
    }

    static void testSupplyAsyncSetRead() throws Exception {
        System.out.println("[测试4] 模拟 ImmutableSkill.execute 场景:");
        System.out.println("  supplyAsync 中设置上下文，然后执行代码读取上下文");
        Map<String, Object> vars = createVars("Player4");

        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("  异步线程: " + Thread.currentThread().getName());

            // 模拟 ImmutableSkill.execute 中的逻辑
            ScriptContext.setCurrent(vars);

            // 模拟 finder() 函数调用
            Object sender = ScriptContext.getSender();
            Object current = ScriptContext.getCurrent();

            System.out.println("  ScriptContext.getCurrent(): " + current);
            System.out.println("  ScriptContext.getSender(): " + sender);

            ScriptContext.clear();
            return sender;
        });

        Object sender = future.get();
        System.out.println("  最终 sender: " + sender);
        System.out.println("  预期: Player4");
        System.out.println("  状态: " + ("Player4".equals(sender) ? "✓ 通过" : "✗ 失败"));
        System.out.println();
    }

    static Map<String, Object> createVars(String sender) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sender", sender);
        return vars;
    }
}
