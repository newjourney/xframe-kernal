package dev.xframe.game.cmd;

import java.lang.reflect.Constructor;

import dev.xframe.game.player.ModularPlayer;
import dev.xframe.injection.ApplicationContext;
import dev.xframe.injection.Injector;
import dev.xframe.modular.ModularEnigne;
import dev.xframe.modular.ModularInjection;
import dev.xframe.modular.ModuleTypeLoader;
import dev.xframe.modular.code.MBridgeBuilder;
import dev.xframe.net.codec.IMessage;
import dev.xframe.tools.LiteParser;

public final class PlayerCmdActionCmd<T extends ModularPlayer> extends PlayerCommand<T>  {

    final Class<?> clazz;
    final Injector injector;
    final Constructor<?> con;
    final ModuleTypeLoader loader;
    final LiteParser liteParser;
    final PlayerCmdInvoker<T> invoker;
    
    @SuppressWarnings("unchecked")
    public PlayerCmdActionCmd(Class<?> clazz) throws Exception {
        this.clazz = clazz;
        this.con = clazz.getConstructor();
        this.loader = ModularEnigne.getLoader(MBridgeBuilder.findModuleType(clazz));
        this.injector = ModularInjection.build(clazz);
        this.liteParser = PlayerCmdLiteAction.class.isAssignableFrom(clazz) ? new LiteParser(clazz, PlayerCmdLiteAction.class) : null;
        this.invoker = ApplicationContext.fetchBean(PlayerCmdInvoker.class);
    }
    
    @Override
    protected void execute0(T player, IMessage req) throws Exception {
        new PlayerCmdInvokeAction<>(invoker, this, player, req, player.queue()).checkin();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void exec(T player, IMessage req) throws Exception {
        PlayerCmdAction<T, ?> action = (PlayerCmdAction<T, ?>) con.newInstance();
        ModularInjection.inject(action, injector, player);
        if(action instanceof PlayerCmdLiteAction) {
        	((PlayerCmdLiteAction) action).parser = liteParser;
        }
        action.exec(player, loader.load(player), req);
    }
    
    @Override
    public Class<?> getClazz() {
        return clazz;
    }

}
