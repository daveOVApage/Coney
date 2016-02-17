package cz.malyzajic.coney.php;

import com.caucho.quercus.env.UnicodeBuilderValue;
import com.sorcix.sirc.Channel;
import com.sorcix.sirc.User;
import cz.malyzajic.coney.Bot;
import cz.malyzajic.coney.bsh.BshBot;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.SimpleBindings;

/**
 *
 * @author daop
 */
public class PhpBot implements Bot {

    private final String name;
    private String nick;
    private final String filename;
    private Channel channel;
    private boolean takeAll = false;
    private PhpInterpreterWrapper interpreter;

    public PhpBot(String name, String filename) {
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

    @Override
    public String getResponse(String request, User sender) {
        String result = null;
        try {
            Bindings bindings = new SimpleBindings();
            bindings.put("request", request);
            bindings.put("sender", sender.getNick());
            UnicodeBuilderValue value = interpreter.eval(UnicodeBuilderValue.class, bindings);
            result = value.getValue();
        } catch (Exception ex) {
            Logger.getLogger(PhpBot.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    @Override
    public boolean takeAll() {
        return takeAll;
    }

    public void setTakeAll(boolean takeAll) {
        this.takeAll = takeAll;
    }

    private void init() {
        try {

            interpreter = new PhpInterpreterWrapper(new File(filename));
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
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "PhpBot{" + "name=" + name + ", nick=" + nick + ", filename=" + filename + ", channel=" + channel + ", takeAll=" + takeAll + '}';
    }

}
