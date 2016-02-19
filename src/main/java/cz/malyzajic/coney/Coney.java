package cz.malyzajic.coney;

import com.sorcix.sirc.Channel;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.IrcDebug;
import com.sorcix.sirc.NickNameException;
import com.sorcix.sirc.PasswordException;
import cz.malyzajic.coney.bsh.BshBot;
import cz.malyzajic.coney.java.WebUpdateChecker;
import cz.malyzajic.coney.php.PhpBot;
import java.io.File;
import java.io.IOException;
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
    public static boolean debug = false;
    public static boolean autoReconnect = false;
    private static String ircHost = "irc.freenode.net";
    private static Options options;
    private static HierarchicalINIConfiguration botsConfig;
    private final String nick;
    private final String nickLowerCase;
    private final String channelName;

    private IrcConnection ircConnection;
    private Channel currentChannal;
    private ConeyListener listener;

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

            options.addOption("help", false, "help");
            options.addOption("n", true, "nick");
            options.addOption("d", false, "debug, default false");
            options.addOption("ch", true, "channel name");
            options.addOption("h", true, "irc host, default irc.freenode.net");
            options.addOption("f", true, "bots INI file");
            options.addOption("a", false, "automatic reconnect after disconnect");

            CommandLineParser parser = new DefaultParser();
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("help")) {
                printHelp();
                return;
            }
            if (line.hasOption("n")) {
                nick = line.getOptionValue("n");
                Logger.getLogger(Coney.class.getName()).log(Level.SEVERE, ("set nick: " + nick));
            }
            if (line.hasOption("ch")) {
                channelName = line.getOptionValue("ch");
            }
            if (line.hasOption("a")) {
                Logger.getLogger(Coney.class.getName()).log(Level.SEVERE, "set autoreconnect ON");
                Coney.autoReconnect = true;
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
                Logger.getLogger(Coney.class.getName()).log(Level.INFO, ("set host: " + Coney.ircHost));
            } else {
                Logger.getLogger(Coney.class.getName()).log(Level.INFO, ("set default host: " + Coney.ircHost));
            }
            if (line.hasOption("d")) {
                Coney.debug = true;
                Logger.getLogger(Coney.class.getName()).log(Level.INFO, "set debug mode ON");
            }
            if (botsConfig != null && !Utils.isEmpty(nick) && !Utils.isEmpty(channelName)) {
                Coney app = new Coney(nick, channelName);
                app.start();
            } else {
                printHelp();
            }

        } catch (ParseException ex) {
            if (options != null) {
                printHelp();
            }
            return;
        }
    }

    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Coney", options);
    }

    private void start() {
        IrcDebug.setEnabled(Coney.debug);
        connect(false);

    }

    public void connect(boolean reset) {
        Logger.getLogger(Coney.class.getName()).log(Level.INFO, "connect reset=" + reset);

        try {
            ircConnection = new IrcConnection(Coney.ircHost);
            listener = new ConeyListener(this);
            ircConnection.addMessageListener(listener);
            ircConnection.setNick(nick);
            ircConnection.connect();
            currentChannal = ircConnection.createChannel(channelName);
            currentChannal.join();

            if (reset) {
                bots.clear();
            }
            createAndPrepareBots(currentChannal);
        } catch (IOException | NickNameException | PasswordException ex) {
            Logger.getLogger(Coney.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void createAndPrepareBots(Channel labka) {
        Set<String> sectionNames = botsConfig.getSections();
        for (String sectionName : sectionNames) {
            SubnodeConfiguration section = botsConfig.getSection(sectionName);
            if (section != null) {

                String type = null;
                String path = null;
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
                if (!Utils.isEmpty(type) && !Utils.isEmpty(sectionName)) {
                    Bot bot = createBot(type, path, sectionName, takeAll, labka);
                    bot.setChannel(labka);
                    System.out.println("add new Bot: " + bot);
                    bots.add(bot);
                }

            }
        }
    }

    private Bot createBot(String type, String path, String nick, boolean takeAll, Channel channel) {
        Bot bot = null;
        if (null != type) {
            switch (type) {
                case "php":
                    if (!Utils.isEmpty(path)) {
                        bot = new PhpBot(nick, path);
                        bot.setTakeAll(takeAll);
                    }
                    break;
                case "bsh":
                    if (!Utils.isEmpty(path)) {
                        bot = new BshBot(nick, path);
                        bot.setTakeAll(takeAll);
                    }
                    break;
                case "web":
                    bot = new WebUpdateChecker(nick);
                    bot.setChannel(channel);
                    break;
                default:
                    break;
            }
        }

        return bot;
    }

    public String getNick() {
        return nick;
    }

    public String getNickLowerCase() {
        return nickLowerCase;
    }

    public List<Bot> getBots() {
        return bots;
    }

}
