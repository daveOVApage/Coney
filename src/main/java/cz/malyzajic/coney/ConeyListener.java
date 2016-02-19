package cz.malyzajic.coney;

import com.sorcix.sirc.Channel;
import com.sorcix.sirc.IrcAdaptor;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.User;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author daop
 */
public class ConeyListener extends IrcAdaptor {

    private final Coney app;

    public ConeyListener(Coney app) {
        this.app = app;
    }

    @Override
    public void onConnect(IrcConnection irc) {

    }

    @Override
    public void onDisconnect(IrcConnection irc) {
        Logger.getLogger(ConeyListener.class.getName()).log(Level.INFO, "onDisconnect");
        if (Coney.autoReconnect) {
            app.connect(true);
        }
    }

    @Override
    public void onPrivateMessage(IrcConnection irc,
            User sender,
            String message) {

    }

    @Override
    public void onMessage(IrcConnection irc,
            User sender,
            Channel target,
            String message) {
        Bot currentBot = null;
        if (message != null && message.toLowerCase().startsWith(app.getNickLowerCase())) {
            if ("bots".equals(message.substring(app.getNickLowerCase().length()).trim())) {
                StringBuilder str = new StringBuilder();
                str.append("Running bots: ");
                int i = 0;
                for (Bot bot : app.getBots()) {
                    if (i++ != 0) {
                        str.append(", ");
                    }
                    str.append(bot.getNick());
                }

                target.send(str.toString());
                return;
            }

            currentBot = findBot(message.substring(app.getNickLowerCase().length()));
            if (currentBot != null) {
                String response = currentBot.getResponse(
                        message.substring(
                                app.getNickLowerCase().length()), sender);

                if (response != null && !response.isEmpty()) {
                    target.sendMessage(response);
                }
            }
        }

        for (Bot bot : app.getBots()) {
            if (bot.takeAll() && !bot.equals(currentBot)) {
                String response = bot.getResponse(message, sender);
                if (response != null && !response.isEmpty()) {
                    target.sendMessage(response);
                }
            }
        }
    }

    private Bot findBot(String message) {
        Bot result = null;
        if (message != null) {
            for (Bot bot : app.getBots()) {
                if (message.trim().toLowerCase().startsWith(bot.getName())) {
                    result = bot;
                    break;
                }
            }
        }
        return result;
    }

}
