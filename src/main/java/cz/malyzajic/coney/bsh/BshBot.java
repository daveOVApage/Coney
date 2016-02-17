package cz.malyzajic.coney.bsh;

import com.sorcix.sirc.Channel;
import com.sorcix.sirc.User;
import cz.malyzajic.coney.Bot;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author daop
 */
public class BshBot implements Bot {

    private final String name;
    private String nick;
    private final String filename;
    private Channel channel;
    private boolean takeAll = false;

    private BshInterpreterWrapper interpreter;

    public BshBot(String name, String filename) {
        this.name = name;
        this.filename = filename;

        init();
    }

    @Override
    public String getNick() {
        return nick;
    }

    @Override
    public String getName() {
        return name;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public BshInterpreterWrapper getInterpreter() {
        return interpreter;
    }

    public boolean isTakeAll() {
        return takeAll;
    }

    @Override
    public void setTakeAll(boolean takeAll) {
        this.takeAll = takeAll;
    }

    public void setInterpreter(BshInterpreterWrapper interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public String getResponse(String request, User sender) {
        String result = null;
        String paramContext = interpreter.createUniqueParamName();
        try {
//            result = getName() + ":: request :: " + request;

            Map<String, Object> context = new HashMap<>();
            context.put("request", request);
            context.put("sender", sender.getNick());
            interpreter.set(paramContext, context);
            result = interpreter.eval(String.class, "worker.getResponse(", paramContext, ")");

        } catch (Exception ex) {
            Logger.getLogger(BshBot.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    private void init() {
        try {
            interpreter = new BshInterpreterWrapper(new File(filename));

            if (nick == null) {
                nick = name;
            }
        } catch (Exception ex) {
            Logger.getLogger(BshBot.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void sendMessage() {

        if (channel != null) {
            channel.sendMessage("test");
        }
    }

    @Override
    public boolean takeAll() {
        return takeAll;
    }

}
