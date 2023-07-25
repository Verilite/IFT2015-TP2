import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Tp2 {

    private HashMap<String, Drug> order = new HashMap<>();
    private PrintWriter writer;
    String currentDate = "";
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    treeNode expiryTree = new treeNode();

    public void processInputFile(String inputFile, String outputFile) {
        try {
            writer = new PrintWriter(outputFile, "UTF-8");
            Scanner scanner = new Scanner(new File(inputFile));
            int prescriptionCount = 0;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                if (line.contains(":") || line.contains(";")) {
                    line = line.replace(":", "");
                    line = line.replace(";", "");
                }

                if (line.isEmpty()) continue;

                StringTokenizer st = new StringTokenizer(line);
                String command = st.nextToken();
                switch (command) {
                    case "APPROV":
                        writer.println("APPROV OK");
                        while (scanner.hasNextLine()) {
                            String approv = scanner.nextLine();
                            if (approv.equals(";")){
                                break;
                            }
                            StringTokenizer st2 = new StringTokenizer(approv);
                            String drugName = st2.nextToken();
                            int quantity = Integer.parseInt(st2.nextToken());
                            Date expiryDate = null;
                            try {
                                expiryDate = new SimpleDateFormat("yyyy-MM-dd").parse(st2.nextToken());
                            } catch (ParseException e) {
                                System.out.println("Invalid date format.");
                            }
                            Drug drug = new Drug(drugName, quantity, expiryDate);
                            int expiryKey = (int) (drug.expiryDate.getTime()/1000);
                            expiryTree.insert(drug, expiryKey);
                            //stock.put(drugName, drug);
                        }
                        break;
                    case "DATE":
                        Date date =  new SimpleDateFormat("yyyy-MM-dd").parse(st.nextToken());
                        boolean check = handleDate(date);
                        if (!check) {
                            writer.println("Invalid date.");
                            writer.close();
                        }
                        currentDate = dateFormat.format(date);
                        if (order.isEmpty()){
                            writer.println(currentDate + " OK");
                        } else {
                            writer.println(currentDate + " COMMANDES:");
                            for (Map.Entry<String, Drug> entry: order.entrySet()){
                                String drugName = entry.getKey();
                                Drug drug = entry.getValue();

                                writer.println(drugName + " " + drug.quantity);
                            }
                        }
                        writer.println();
                        break;
                    case "STOCK":
                        handleStock();
                        writer.println();
                        break;
                    case "PRESCRIPTION":
                        writer.println("PRESCRIPTION " + (++prescriptionCount));
                        while (scanner.hasNextLine()) {
                            String nextLine = scanner.nextLine();
                            if (nextLine.equals(";")){
                                break;
                            }
                            StringTokenizer st2 = new StringTokenizer(nextLine);
                            String drugName = st2.nextToken();
                            int quantity = Integer.parseInt(st2.nextToken());
                            int days = Integer.parseInt(st2.nextToken());
                            printPrescriptionStatus(drugName, quantity, days);
                        }
                        writer.println();
                        break;
                }
            }
            scanner.close();
            writer.close();
        } catch (IOException e) {
            System.out.println("An error occurred while trying to read the file.");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    private void insertOrder(String key, Drug drug){
        if (order.containsKey(key)){
            order.get(key).quantity += drug.quantity;
        } else {
            order.put(key, drug);
        }
    }

    public boolean handleDate(Date date) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            df.setLenient(false);
            df.parse(dateFormat.format(date));
            currentDate = dateFormat.format(date);
            removeExpiredDrugs(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public void removeExpiredDrugs(Date date){
        removeExpiredDrugsRec(expiryTree.root, date);
    }

    public void removeExpiredDrugsRec(treeNode.Node node, Date date){
        if (node == null){
            return;
        }

        removeExpiredDrugsRec(node.left, date);

        Iterator<Drug> iter = node.drugs.iterator();
        while (iter.hasNext()){
            Drug drug = iter.next();
            if (drug != null && !drug.expiryDate.after(date)){
                iter.remove();
            }
        }

        removeExpiredDrugsRec(node.right, date);

    }

    public void handleStock() throws ParseException {
        writer.println("STOCK " + currentDate);
        printStockRec(expiryTree.root);
    }

    private void printStockRec(treeNode.Node node) throws ParseException{
        if (node == null){
            return;
        }

        printStockRec(node.left);

        Iterator<Drug> iter = node.drugs.iterator();
        while (iter.hasNext()){
            Drug drug = iter.next();
            if (drug != null && !drug.expiryDate.before(dateFormat.parse(currentDate))){
                writer.println(drug.name + " " + drug.quantity + " " + dateFormat.format(drug.expiryDate));
            }
        }

        printStockRec(node.right);
    }

    public void printPrescriptionStatus(String drugName, int quantity, int days) {
        int remainingQty = dispenseDrug(drugName, quantity * days);
        if (remainingQty == 0){
            writer.println(drugName + " " + quantity + " " + days + " OK");
        } else {
            writer.println(drugName + " " + quantity + " " + days + " COMMANDE");
            insertOrder(drugName, new Drug(drugName, remainingQty, null));
        }
    }

    public int dispenseDrug(String drugName, int quantity){
        return dispenseDrugRec(expiryTree.root, drugName, quantity);
    }

    private int dispenseDrugRec(treeNode.Node node, String drugName, int quantity){
        if (node == null){
            return quantity;
        }

        quantity = dispenseDrugRec(node.left, drugName, quantity);

        if (quantity <= 0){
            return 0;
        }

        Iterator<Drug> iter = node.drugs.iterator();
        while (iter.hasNext()){
            Drug drug = iter.next();
            if (drug.name.equals(drugName)){
                if (drug.quantity > quantity) {
                    drug.quantity -= quantity;
                    return 0;
                } else {
                    quantity -= drug.quantity;
                    if (quantity <= 0){
                        iter.remove();
                    }
                }
            }
        }

        return dispenseDrugRec(node.right, drugName, quantity);
    }

    public static void main(String[] args) {
        Tp2 tp = new Tp2();
        tp.processInputFile("src/test1.txt", "src/test1+.txt");
    }

    private static class Drug {

        String name;
        int quantity;
        Date expiryDate;

        public Drug(String name, int quantity, Date expiryDate) {
            this.name = name;
            this.quantity = quantity;
            this.expiryDate = expiryDate;
        }
    }

    private static class treeNode {

        private Drug data;
        private treeNode left;
        private treeNode right;
        static class Node {
            int key;
            List<Drug> drugs;
            Node left, right;

            public Node(Drug drug, int item){
                key = item;
                drugs = new ArrayList<>();
                drugs.add(drug);
                left = right = null;
            }
        }
        Node root;

        public treeNode(){
            root = null;
        }
        public Drug getData() {
            return data;
        }
        public void setData(Drug data) {
            this.data = data;
        }
        public treeNode getLeft() {
            return left;
        }
        public void setLeft(treeNode left) {
            this.left = left;
        }
        public treeNode getRight() {
            return right;
        }

        public void setRight(treeNode right) {
            this.right = right;
        }

        public treeNode(Drug drug, int key){
            root = new Node(drug, key);
        }
        public void insert(Drug drug, int key){
            root = insertRec(root, drug, key);
        }

        public Node insertRec(Node root, Drug drug, int key){
            if (root == null){
                return new Node(drug, key);
            } else if (key < root.key) {
                root.left = insertRec(root.left, drug, key);
            } else if (key > root.key){
                root.right = insertRec(root.right, drug, key);
            } else {
                root.drugs.add(drug);
            }
            return root;
        }

        public Node find(Node root, int key){
            if (root == null || root.key == key){
                return root;
            }
            if (root.key < key){
                return find(root.right, key);
            }
            return find(root.left, key);
        }

        public Node delete(Node root, int key){
            if (root == null){
                return root;
            }

            if (root.key > key){
                root.left = delete(root.left, key);
                return root;
            } else if (root.key < key) {
                root.right = delete(root.right, key);
                return root;
            }

            if (root.left == null){
                Node temp = root.right;
                return temp;
            } else if (root.right == null){
                Node temp = root.left;
                return temp;
            } else {
                Node succParent = root;
                Node succ = root.right;
                while (succ.left != null){
                    succParent = succ;
                    succ = succ.left;
                }

                if (succParent != root){
                    succParent.left = succ.right;
                } else {
                    succParent.right = succ.right;
                }

                root.key = succ.key;

                return root;
            }
        }
    }

}
