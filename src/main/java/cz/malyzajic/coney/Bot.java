package cz.malyzajic.coney;

import com.sorcix.sirc.Channel;
import com.sorcix.sirc.User;

/**
 *
 * @author daop
 */
public interface Bot {

    String getNick();

    String getName();

    String getResponse(String request, User sender);

    boolean takeAll();

    void setTakeAll(boolean takeAll);

    public void setChannel(Channel channel);

}
