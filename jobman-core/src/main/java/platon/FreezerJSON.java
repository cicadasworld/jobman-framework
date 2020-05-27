package platon;


public interface FreezerJSON {

    // 将当前消息打包成JSON树, 返回JSON树的指针
    JsonNode freezeToJSON() throws FreezeException;

    // 从给定JSON树中解包, 并将解包得到的数据覆盖到当前消息上。
    void defreezeFromJSON(JsonNode inputJsonTree) throws DefreezeException;
}
