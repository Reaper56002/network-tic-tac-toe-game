package com.groupMProject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class TicTacToe implements Runnable {

    private String ip = "localhost";
    private int port = 135;
    private Scanner scanner = new Scanner(System.in);
    private JFrame frame;
    private final int WIDTH = 506;
    private final int HEIGHT = 527;
    private Thread thread;
    private Painter painter;
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private ServerSocket serverSocket;

    private BufferedImage board;
    private BufferedImage redX;
    private BufferedImage blueX;
    private BufferedImage redCircle;
    private BufferedImage blueCircle;
    private String[] spaces = new String[9];

    private boolean yourTurn = false;
    private boolean circle = true;
    private boolean accepted = false;
    private boolean failedToCommunicate = false;
    private boolean won = false;
    private boolean enemyWon = false;
    private boolean tie = false;

    private int sizeSpace = 160;
    private int numErrors = 0;
    private int firstMove = -1;
    private int secondMove = -1;

    private Font font = new Font("Cambria", Font.BOLD, 32);
    private Font smallerFont = new Font("Cambria", Font.BOLD, 20);
    private Font largerFont = new Font("Cambria", Font.BOLD, 50);

    private String waitingString = "Waiting for opponent";
    private String failedToCommunicateString = "Failed to communicate.";
    private String wonString = "You won!";
    private String enemyWonString = "You Lost!";
    private String tieString = "TIE";

    private int[][] wins = new int[][]
            { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };


    public TicTacToe() {
        System.out.println("Connell is Proud to Present" + '\n' +"Tic-Tac-Toe" + '\n' +
                "Networking Enabled" + '\n'+ "Please enter your desired IP: ");
        ip = scanner.nextLine();
        System.out.println("Port: ");
        port = scanner.nextInt();
        while (port < 1 || port > 80000) {
            System.out.println("Port invalid, input another port: ");
            port = scanner.nextInt();
        }
        loadImages();
        painter = new Painter();
        painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        if (!connect()) initializeServer();
        frame = new JFrame();
        frame.setTitle("Tic-Tac-Toe");
        frame.setContentPane(painter);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
        thread = new Thread(this, "TicTacToe");
        thread.start();
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        TicTacToe ticTacToe = new TicTacToe();
    }

    public void run() {
        while (true) {
            tick();
            painter.repaint();
            if (!circle && !accepted) {
                listenForServerRequest();
            }
        }
    }


    private void loadImages() {
        try {
            board = ImageIO.read(getClass().getResourceAsStream("/board.png"));
            redX = ImageIO.read(getClass().getResourceAsStream("/redX.png"));
            redCircle = ImageIO.read(getClass().getResourceAsStream("/redCircle.png"));
            blueX = ImageIO.read(getClass().getResourceAsStream("/blueX.png"));
            blueCircle = ImageIO.read(getClass().getResourceAsStream("/blueCircle.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void render(Graphics g) {
        g.drawImage(board, 0, 0, null);
        if (failedToCommunicate) {
            g.setColor(Color.RED);
            g.setFont(smallerFont);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(failedToCommunicateString);
            g.drawString(failedToCommunicateString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            return;
        }

        if (accepted)
        {
            for (int i = 0; i < spaces.length; i++) {
                if (spaces[i] != null) {
                    if (spaces[i].equals("X"))
                    {
                        if (circle) {
                            g.drawImage(redX, (i % 3) * sizeSpace + 10 * (i % 3), (int) (i / 3) * sizeSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(blueX, (i % 3) * sizeSpace + 10 * (i % 3), (int) (i / 3) * sizeSpace + 10 * (int) (i / 3), null);
                        }
                    } else if (spaces[i].equals("O")) {
                        if (circle) {
                            g.drawImage(blueCircle, (i % 3) * sizeSpace + 10 * (i % 3), (int) (i / 3) * sizeSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(redCircle, (i % 3) * sizeSpace + 10 * (i % 3), (int) (i / 3) * sizeSpace + 10 * (int) (i / 3), null);
                        }
                    }
                }
            }

            if (won || enemyWon) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(10));
                g.setColor(Color.BLACK);
                g.drawLine(firstMove % 3 * sizeSpace + 10 * firstMove % 3 + sizeSpace / 2, (int) (firstMove / 3) * sizeSpace + 10 * (int)
                        (firstMove / 3) + sizeSpace / 2, secondMove % 3 * sizeSpace + 10 * secondMove % 3 + sizeSpace / 2, (int) (secondMove / 3)
                        * sizeSpace + 10 * (int) (secondMove / 3) + sizeSpace / 2);
                g.setColor(Color.RED);
                g.setFont(largerFont);
                if (won) {
                    int stringWidth = g2.getFontMetrics().stringWidth(wonString);
                    g.drawString(wonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                } else if (enemyWon) {
                    int stringWidth = g2.getFontMetrics().stringWidth(enemyWonString);
                    g.drawString(enemyWonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                }
            }
            if (tie) {
                Graphics2D g2 = (Graphics2D) g;
                g.setColor(Color.BLACK);
                g.setFont(largerFont);
                int stringWidth = g2.getFontMetrics().stringWidth(tieString);
                g.drawString(tieString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            }
        } else {
            g.setColor(Color.RED);
            g.setFont(font);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(waitingString);
            g.drawString(waitingString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
        }
    }



    private void tick() {
        if (numErrors >= 10) failedToCommunicate = true;

        if (!yourTurn && !failedToCommunicate) {
            try {
                int space = dis.readInt();
                if (circle) spaces[space] = "X";
                else spaces[space] = "O";
                checkForEnemyWin();
                checkForTie();
                yourTurn = true;
            } catch (IOException e) {
                e.printStackTrace();
                numErrors++;
            }
        }
    }

    private void checkForWin() {
        for (int i = 0; i < wins.length; i++) {
            if (circle) {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstMove = wins[i][0];
                    secondMove = wins[i][2];
                    won = true;
                }
            } else {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstMove = wins[i][0];
                    secondMove = wins[i][2];
                    won = true;
                }
            }
        }
    }

    private void checkForEnemyWin() {
        for (int i = 0; i < wins.length; i++) {
            if (circle) {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstMove = wins[i][0];
                    secondMove = wins[i][2];
                    enemyWon = true;
                }
            } else {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstMove = wins[i][0];
                    secondMove = wins[i][2];
                    enemyWon = true;
                }
            }
        }
    }

    private void checkForTie() {
        for (int i = 0; i < spaces.length; i++) {
            if (spaces[i] == null) {
                return;
            }
        }
        tie = true;
    }


    private void listenForServerRequest() {
        Socket socket = null;
        try {
            socket = serverSocket.accept();
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            accepted = true;
            System.out.println("CLIENT JOINED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean connect() {
        try {
            socket = new Socket(ip, port);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            accepted = true;
        } catch (IOException e) {
            System.out.println("Unable to connect to the address: " + ip + ":" + port + " | Starting a server");
            return false;
        }
        System.out.println("Successfully connected to the server.");
        return true;
    }

    private void initializeServer() {
        try {
            serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
        } catch (Exception e) {
            e.printStackTrace();
        }
        yourTurn = true;
        circle = false;
    }

    public class Painter extends JPanel implements MouseListener {
        private static final long serialVersionUID = 1L;
        public Painter() {
            setFocusable(true);
            requestFocus();
            setBackground(Color.WHITE);
            addMouseListener(this);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            render(g);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (accepted) {
                if (yourTurn && !failedToCommunicate && !won && !enemyWon) {
                    int x = e.getX() / sizeSpace;
                    int y = e.getY() / sizeSpace;
                    y *= 3;
                    int position = x + y;
                    if (spaces[position] == null) {
                        if (!circle) spaces[position] = "X";
                        else spaces[position] = "O";
                        yourTurn = false;
                        repaint();
                        Toolkit.getDefaultToolkit().sync();
                        try {
                            dos.writeInt(position);
                            dos.flush();
                        } catch (IOException e1) {
                            numErrors++;
                            e1.printStackTrace();
                        }
                        System.out.println("DATA WAS SENT");
                        checkForWin();
                        checkForTie();
                    }
                }
            }
        }
        @Override
        public void mousePressed(MouseEvent e) {
        }
        @Override
        public void mouseReleased(MouseEvent e) {
        }
        @Override
        public void mouseEntered(MouseEvent e) {
        }
        @Override
        public void mouseExited(MouseEvent e) {
        }
    }
}
