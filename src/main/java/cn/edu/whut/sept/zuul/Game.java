package cn.edu.whut.sept.zuul;

import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.infra.WorldFactory;

/**
 * 旧版 CLI 游戏循环（保留编译兼容，正式入口为 {@link cn.edu.whut.sept.zuul.client.DesktopLauncher}）。
 */
public class Game
{
    private Parser parser;
    private Room currentRoom;

    public Game()
    {
        currentRoom = WorldFactory.build();
        parser = new Parser();
    }

    public void play()
    {
        printWelcome();

        boolean finished = false;
        while (!finished) {
            Command command = parser.getCommand();
            if (command == null) {
                System.out.println("I don't understand...");
            } else {
                finished = command.execute(this);
            }
        }

        System.out.println("Thank you for playing.  Good bye.");
    }

    private void printWelcome()
    {
        System.out.println();
        System.out.println("Welcome to the World of Zuul!");
        System.out.println("World of Zuul is a new, incredibly boring adventure game.");
        System.out.println("Type 'help' if you need help.");
        System.out.println();
        System.out.println(currentRoom.getLongDescription());
    }

    public Room getCurrentRoom()
    {
        return currentRoom;
    }

    public void setCurrentRoom(Room room)
    {
        this.currentRoom = room;
    }
}
