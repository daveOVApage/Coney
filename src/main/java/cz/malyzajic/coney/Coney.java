package cz.malyzajic.coney;

import com.sorcix.sirc.Channel;
import com.sorcix.sirc.IrcAdaptor;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.IrcDebug;
import com.sorcix.sirc.User;
import cz.malyzajic.coney.bsh.BshBot;
import cz.malyzajic.coney.php.PhpBot;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

/**
 *
 * @author daop
 */
public class Coney {

    private final List<Bot> bots = new ArrayList<>(5);
    private static boolean debug = false;
    private static String ircHost = "irc.freenode.net";
    private static Options options;
    private static HierarchicalINIConfiguration botsConfig;
    private final String nick;
    private final String nickLowerCase;
    private final String channelName;

    public Coney(String nick, String channelName) {
        this.nick = nick;
        this.nickLowerCase = nick.toLowerCase();
        this.channelName = channelName;
    }

    public static void main(String[] args) {
        String nick = null;
        String channelName = null;
        File iniFile;
        try {
            options = new Options();

            options.addOption("h", false, "help");
            options.addOption("n", true, "nick");
            options.addOption("d", false, "debug, default false");
            options.addOption("ch", true, "channel name");
            options.addOption("h", true, "irc host, default irc.freenode.net");
            options.addOption("f", true, "bots INI file");

            CommandLineParser parser = new DefaultParser();
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Coney", options);
            }
            if (line.hasOption("n")) {
                nick = line.getOptionValue("n");
                System.out.println("set nick: " + nick);
            }
            if (line.hasOption("ch")) {
                channelName = line.getOptionValue("ch");
            }
            if (line.hasOption("f")) {
                String iniString = line.getOptionValue("f");
                iniFile = new File(iniString);
                if (iniFile.exists()) {
                    try {
                        botsConfig = new HierarchicalINIConfiguration(iniFile);
                    } catch (ConfigurationException ex) {
                        Logger.getLogger(Coney.class.getName()).log(Level.SEVERE, null, ex);
                        botsConfig = null;
                    }

                }

            }

            if (line.hasOption("h")) {
                Coney.ircHost = line.getOptionValue("h");
                System.out.println("set host: " + Coney.ircHost);
            } else {
                System.out.println("default host: " + Coney.ircHost);
            }
            if (line.hasOption("d")) {
                Coney.debug = true;
                System.out.println("set debug mode ON");
            }
            if (botsConfig != null && !Utils.isEmpty(nick) && !Utils.isEmpty(channelName)) {
                Coney app = new Coney(nick, channelName);
                app.start();
            }

        } catch (ParseException ex) {
            if (options != null) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Coney", options);
            }
        }
    }

    private void start() {
        try {

            IrcDebug.setEnabled(Coney.debug);
            IrcConnection ic = new IrcConnection(Coney.ircHost);

            ic.addMessageListener(new IrcAdaptor() {
                @Override
                public void onConnect(IrcConnection irc) {

                }

                @Override
                public void onPrivateMessage(IrcConnection irc,
                        User sender,
                        String message) {
                    sender.sendMessage("cau, slysim te i soukrome");
                }

                @Override
                public void onMessage(IrcConnection irc,
                        User sender,
                        Channel target,
                        String message) {
                    Bot currentBot = null;
                    if (message != null && message.toLowerCase().startsWith(nickLowerCase)) {
                        currentBot = findBot(message.substring(nickLowerCase.length()));
                        if (currentBot != null) {
                            String response = currentBot.getResponse(
                                    message.substring(
                                            nickLowerCase.length()), sender);

                            if (response != null && !response.isEmpty()) {
                                target.sendMessage(response);
                            }
                        } else {
                            target.sendMessage(" Boti nezabrali. mas tam preklep, a nebo pises zbytecnosti");
                        }

                    }
                    for (Bot bot : bots) {
                        if (bot.takeAll() && !bot.equals(currentBot)) {
                            String response = bot.getResponse(message, sender);
                            if (response != null && !response.isEmpty()) {
                                target.sendMessage(response);
                            }
                        }
                    }
                }

            });

            ic.setNick(nick);
            ic.connect();
            Channel labka = ic.createChannel(channelName);
            labka.join();
            createAndPrepareBots(labka);
        } catch (Exception ex) {
            Logger.getLogger(Coney.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private Bot findBot(String message) {
        Bot result = null;
        if (message != null) {
            for (Bot bot : bots) {
                if (message.trim().toLowerCase().startsWith(bot.getName())) {
                    result = bot;
                    break;
                }
            }
        }
        return result;
    }

    private void createAndPrepareBots(Channel labka) {
        Set<String> sectionNames = botsConfig.getSections();
        for (String sectionName : sectionNames) {
            SubnodeConfiguration section = botsConfig.getSection(sectionName);
            if (section != null) {

                String type = null;
                String path = null;
                String nick = sectionName;
                boolean takeAll = false;

                Iterator<String> keys = section.getKeys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = section.getString(key);
                    if (value != null && key != null) {
                        switch (key) {
                            case "type":
                                type = value;
                                break;
                            case "path":
                                path = value;
                                break;
                            case "takeAll":
                                takeAll = Boolean.parseBoolean(value);
                                break;
                            default:
                                break;
                        }
                    }
                }
                if (!Utils.isEmpty(path) && !Utils.isEmpty(type) && !Utils.isEmpty(nick)) {
                    Bot bot = createBot(type, path, nick, takeAll);
                    bot.setChannel(labka);
                    System.out.println("add new Bot: " + bot);
                    bots.add(bot);
                }

            }
        }
    }

    private Bot createBot(String type, String path, String nick, boolean takeAll) {
        Bot bot = null;
        if ("php".equalsIgnoreCase(type)) {
            bot = new PhpBot(nick, path);
            bot.setTakeAll(takeAll);
        } else if ("bsh".equalsIgnoreCase(type)) {
            bot = new BshBot(nick, path);
            bot.setTakeAll(takeAll);
        }

        return bot;
    }
}
