import com.sun.source.tree.Tree;

import java.io.FileOutputStream;
import java.util.*;
class TreeNode{
    private static int idCounter = 0;
    int id;
    String value;
    List<TreeNode> children;

    public TreeNode(String value) {
        this.id=idCounter++;
        this.value = value;
        this.children=new ArrayList<>();
    }

    public void addChild(TreeNode child) {
        this.children.add(child);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

class ParserTree{
    TreeNode root;
    public ParserTree(String rootValue) {
        this.root = new TreeNode(rootValue);
    }

    public void addChild(TreeNode parent,TreeNode child) {
//        TreeNode parentNode = findNode(root, parentValue);
//        if (parentNode != null) {
//            TreeNode childNode = new TreeNode(childValue);
//            parentNode.addChild(childNode);
//        } else {
//            System.out.println("Parent node not found.");
//        }
        parent.addChild(child);
    }

    private TreeNode findNode(TreeNode node, String value) {
        if (node == null) {
            return null;
        }
        if (node.value.equals(value)) {
            return node;
        }
        for (TreeNode child : node.children) {
            TreeNode foundNode = findNode(child, value);
            if (foundNode != null) {
                return foundNode;
            }
        }
        return null;
    }

    public void printTree() {
        printTree(this.root, 0);
    }

    private void printTree(TreeNode node, int level) {
        if (node == null) {
            return;
        }
        // 打印当前节点及其层级缩进
        for (int i = 0; i < level; i++) {
            System.out.print("\t");
        }
        System.out.println(node.value);
        // 递归打印子节点
        for (TreeNode child : node.children) {
            printTree(child, level + 1);
        }
    }
}

class Production{
    //产生式左部
    String left;
    //产生式右部：一个|
    List<String> right;//把单个产生式右边的字符串以空格分割开

    public Production(String left, List<String> right) {
        this.right = right;
        this.left = left;
    }

    @Override
    public String toString() {
        return "Production{" +
                "left='" + left + '\'' +
                ", right=" + right +
                '}';
    }
}

class Constants{
    public static final String[] PRODUCTION_RULES=new String[]{
            "program -> compoundstmt",
            "stmt ->  ifstmt  |  whilestmt  |  assgstmt  |  compoundstmt",
            "compoundstmt ->  { stmts }",
            "stmts ->  stmt stmts   |   E",
            "ifstmt ->  if ( boolexpr ) then stmt else stmt",
            "whilestmt ->  while ( boolexpr ) stmt",
            "assgstmt ->  ID = arithexpr ;",
            "boolexpr  ->  arithexpr boolop arithexpr",
            "boolop ->   <  |  >  |  <=  |  >=  | ==",
            "arithexpr  ->  multexpr arithexprprime",
            "arithexprprime ->  + multexpr arithexprprime  |  - multexpr arithexprprime  |   E",
            "multexpr ->  simpleexpr  multexprprime",
            "multexprprime ->  * simpleexpr multexprprime  |  / simpleexpr multexprprime  |   E",
            "simpleexpr ->  ID  |  NUM  |  ( arithexpr )"
    };
    public static final List<String> NONTERMINALS= List.of(new String[]{
            "program", "stmt", "compoundstmt", "stmts", "ifstmt", "whilestmt", "assgstmt", "boolexpr",
            "boolop", "arithexpr", "arithexprprime", "multexpr", "multexprprime", "simpleexpr"
    });
    public static final List<String> TERMINALS= List.of(new String[]{
            "{", "}", "if", "(", ")", "then", "else", "while", "ID", "<", ">", "<=", ">=",
            "==", "+", "-", "*", "/", "NUM", "E",";","=","$"
    });
    public static final String START="program";

}


public class Main {
    private static StringBuffer prog = new StringBuffer();

    private static List<Production> productions=new ArrayList<>();
    private static HashMap<String, HashSet<String>> firstSet=new HashMap<>();
    private static HashMap<String,HashSet<String>> followSet=new HashMap<>();
    private static HashMap<String,HashSet<String>> followDependency=new HashMap<>();
    private static Map<String, Map<String, Production>> parsingTable = new HashMap<>();
    private static Stack<TreeNode> parserStack=new Stack<>();
    private static List<String> input=new ArrayList<>();
    private static ParserTree parserTree=new ParserTree(Constants.START);


    /**
     *  this method is to read the standard input
     */
    private static void read_prog()
    {
        Scanner sc = new Scanner(System.in);
        String line;

        while (sc.hasNextLine()) {
            line = sc.nextLine();
            if (line.equals("EOF")) {//用EOF作为结束标志
                break;
            }
            prog.append(line).append("\n");
        }
    }


    // add your method here!!



    /**
     *  you should add some code in this method to achieve this lab
     */
    private static void analysis()
    {
        read_prog();
        System.out.print(prog);
        constructParsingTable();
        constructParsingTree();
        parserTree.printTree();
    }

    private static void constructParsingTree() {
        splitProg();
        initStack();
        parse();
    }

    private static void parse(){
        int idx=0;
        String inputTop=input.get(idx);
        String stackTop=parserStack.peek().getValue();
        while(!stackTop.equals("$")){
            if(Constants.TERMINALS.contains(stackTop)){
                if(stackTop.equals(inputTop)){
                    //两个栈顶的终结符相同
                    parserStack.pop();
                    stackTop=parserStack.peek().getValue();//更新stack栈顶node值
                    idx++;
                    inputTop=input.get(idx);
                }
                else{
                    //stack栈顶是非终结符，但是input栈顶没有匹配的，加上stack栈顶元素
                    error(idx,stackTop);
                    input.add(idx,stackTop);
                    inputTop=input.get(idx);
                }
            }
            else{
                Production production=parsingTable.get(stackTop).get(inputTop);
                if(production!=null){
                    //parsingTable中能找到对应的产生式
                    TreeNode parent=parserStack.pop();
                    //逆序压栈
                    List<String> productionRight=production.right;
                    for(int i=productionRight.size()-1;i>=0;i--){
                        TreeNode child=new TreeNode(productionRight.get(i));
                        if(!productionRight.get(i).equals("E")){
                            //如果非空，逆序压栈；如果是空，不压栈
                            parserStack.push(child);
                        }
                        //构造语法树
                        parserTree.addChild(parent,child);
                    }
                    stackTop=parserStack.peek().getValue();
                }
                else{
                    error(idx," ");
                }



            }
        }
    }

    private static void error(int idx,String missing){
        //获取行数
        int line=0;
        String[] progSplitByEnter=prog.toString().split("\n");
        for(String progLine:progSplitByEnter){
            line++;
            if(progLine.contains(missing)){
                break;
            }
        }
        System.out.println("语法错误,第"+line+"行,缺少\""+missing+"\"");
    }

    private static void splitProg() {
        input=new ArrayList<>( Arrays.asList(prog.toString().split("\\s+")));//表示匹配一个或多个空白字符（包括空格、制表符、换行符等）List.of是不可变列表,再add就会报错
        input.add("$");//末尾加上终止符
//        for(String s:input){
//            System.out.println(s);
//        }
    }

    private static void initStack() {
        parserStack.push(new TreeNode("$"));
        parserStack.push(parserTree.root);
    }

    private static void constructParsingTable() {
        //把字符串数组常量production_rules转变为Production对象数组
        constructProductions(Constants.PRODUCTION_RULES);
        //计算first集
        for(String nonTerminal:Constants.NONTERMINALS){
            computeFirst(nonTerminal);
        }
        //打印first集
        System.out.println("FIRST");
        for (Map.Entry<String, HashSet<String>> entry : firstSet.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
        //计算follow集
        for(String nonTerminal:Constants.NONTERMINALS){
            computeFollow(nonTerminal);
        }
        //计算follow集的时候维护了一个依赖关系，为了避免循环调用，所以这里还要再加上
        for(Map.Entry<String, HashSet<String>> entry : followDependency.entrySet()){
            String nonTerminal= entry.getKey();
            HashSet<String> dependedNT=entry.getValue();
            for(String depend:dependedNT){
                HashSet<String> followSetOfDependedNT=followSet.get(depend);
                followSet.get(nonTerminal).addAll(followSetOfDependedNT);
            }
        }
        //打印follow集
        System.out.println("FOLLOW");
        for (Map.Entry<String, HashSet<String>> entry : followSet.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
        //构造LL(1)ParsingTable
        for(Production production:productions){
            String rowKey=production.left;
            //TODO:计算产生式右边的first集
            HashSet<String> rightPartFirstSet=computeFirstSet4RightPart(production.right);//不应该直接用左部的first集代替右边的，因为左边的可能有多条产生式
            for(String firstElement:rightPartFirstSet){
                if(!firstElement.equals("E")){
                    parsingTable.computeIfAbsent(rowKey,k->new HashMap<>()).put(firstElement,production);
                }
                else{
                    HashSet<String> leftPartFollowSet=followSet.get(production.left);
                    for(String followElement:leftPartFollowSet){
                        parsingTable.computeIfAbsent(rowKey,k->new HashMap<>()).put(followElement,production);
                    }
                }
            }
        }
        //打印ParsingTable
        System.out.println("Table");
        for (Map.Entry<String, Map<String, Production>> row : parsingTable.entrySet()) {
            String rowKey = row.getKey();
            Map<String, Production> columns = row.getValue();
            for (Map.Entry<String, Production> col : columns.entrySet()) {
                String colKey = col.getKey();
                Production production = col.getValue();
                System.out.println("[" + rowKey + "][" + colKey + "] = " + production);
            }
        }

    }

    private static HashSet<String> computeFirstSet4RightPart(List<String> right) {
        HashSet<String> set=new HashSet<>();
        boolean deleteE=false;
        for(String element:right){
            if(Constants.TERMINALS.contains(element)){
                set.add(element);
                return set;
            }
            else{
                set.addAll(firstSet.get(element));
                if(!firstSet.get(element).contains("E")){
                    deleteE=true;
                    break;
                }
            }
        }
        if(deleteE) set.remove("E");
        return set;
    }

    private static void computeFollow(String nonTerminal) {
        if(followSet.containsKey(nonTerminal)) return;//已经计算过follow集
        HashSet<String> expr=new HashSet<>();
        if(nonTerminal.equals(Constants.START)){//起始符
            expr.add("$");
        }
        //找所有右部有nonterminal的
        for(Production production:productions){
            if(production.right.contains(nonTerminal)){

                int length=production.right.size();
                int idx=production.right.indexOf(nonTerminal);
                if(idx==length-1){
                    if(!production.left.equals( nonTerminal)){
                        //说明是最后一个，A->aB的情况，FOLLOW(A) is in F0LLOW(B)
                        //添加计算follow集的依赖关系，计算Follow(B)需要Follow(A)
                        addFollowDependency(nonTerminal,production.left);
                        //如果依赖关系中存在该产生式左边元素对右边元素的依赖，且follow集已计算的中间还没有完成对右边该元素的计算，说明存在循环调用
                        if(followDependency.containsKey(production.left)&&followDependency.get(production.left).contains(nonTerminal)&&!followSet.containsKey(production.left)) break;
                        computeFollow(production.left);
                        expr.addAll(followSet.get(production.left));
                    }
                    //A->aA忽略

                }
                else{
                    //说明是A->aBb的情况，先计算First(b),如果First(B)中含有E，那么就把first集中除了E加入Follow(B)且把Follow(A)的所有都加入Follow(B),否则就把First(b)加入就行
                    //得到First(b):不能用firstSet.get(production.right.get(idx+1));因为可能是非终结符要判断是否是非终结符
                    String nextSymbol=production.right.get(idx+1);
                    HashSet<String> nextFirstSet=new HashSet<>();
                    if(Constants.NONTERMINALS.contains(nextSymbol)){
                        nextFirstSet=firstSet.get(nextSymbol);
                    }
                    else if(Constants.TERMINALS.contains(nextSymbol)){
                        nextFirstSet.add(nextSymbol);
                    }
                    if(nextFirstSet.contains("E")){
                        //把nextFirstSet中所有不是E的加入到expr中
                        HashSet<String> withoutE=new HashSet<>();
                        for(String s:nextFirstSet){
                            if(!s.equals("E")){
                                withoutE.add(s);
                            }
                        }
                        expr.addAll(withoutE);
                        //把Follow(A)的所有都加入Follow(B)
                        addFollowDependency(nonTerminal,production.left);
                        computeFollow(production.left);
                        expr.addAll(followSet.get(production.left));
                    }
                    else{
                        expr.addAll(nextFirstSet);
                    }
                }
            }
        }
        followSet.put(nonTerminal,expr);
    }

    private static void addFollowDependency(String nonTerminal, String depended) {
        if(followDependency.containsKey(nonTerminal)){
            followDependency.get(nonTerminal).add(depended);
        }
        else{
            HashSet<String> depends=new HashSet<>();
            depends.add(depended);
            followDependency.put(nonTerminal,depends);
        }
    }

    private static void computeFirst(String nonTerminal) {
        if(firstSet.containsKey(nonTerminal)) return;//已经计算过这个非终结符的first集
        HashSet<String> expr=new HashSet<>();//对于非终结符nonTerminal的first集
        //找到所有以nonTerminal为左部的产生式
        for(Production production:productions){
            if(production.left.equals(nonTerminal)){
                int length=production.right.size();
                int idx=0;//用于计数，一条产生式右部被分成了多个String
                String curStmt=production.right.get(idx);
                if(Constants.TERMINALS.contains(curStmt)){//包括终结符和空
                    expr.add(curStmt);
                }
                else if(Constants.NONTERMINALS.contains(curStmt)){//非终结符的情况X->Y0Y1Y2...
                    computeFirst(curStmt);//0，先计算第一个元素Y0的first集
                    //专门用来存放非终结符这种情况的first集，即把First(Yi)不包括E之前的所有first即加入
                    HashSet<String> nTFirstSet = new HashSet<>(firstSet.get(curStmt));
                    while(idx<length-1 && firstSet.get(curStmt).contains("E")){//确认下标合法且上一个Yi的first集中有空
                        idx++;
                        curStmt=production.right.get(idx);
                        computeFirst(curStmt);
                        nTFirstSet.addAll(firstSet.get(curStmt));
                    }
                    //Attention:只有当E存在于所有First(Yi)中才要加E进入First(Y0Y1...)
                    if(idx!=length-1||!firstSet.get(curStmt).contains("E")){//判断退出循环的时候是否遍历了所有的Yi，且最后一个的First集也包含E
                        nTFirstSet.remove("E");
                    }
                    expr.addAll(nTFirstSet);//在最终的first集中加入非终结符的这个情况
                }
            }
        }
        firstSet.put(nonTerminal,expr);
    }

    private static void constructProductions(String[] productionRules) {
        for(String rule:productionRules){
            String[] parts=rule.split("->");
            if(parts.length==2){
                String leftPart=parts[0].trim();
                String[] singleRightParts=parts[1].trim().split("\\|");//右部的|分割
                for(String singleRightPart:singleRightParts){
                    Production productionRule=new Production(leftPart, List.of(singleRightPart.trim().split("\\s+")));
                    productions.add(productionRule);
                }
            }
            else{
                System.out.println("The production rule is not in the expected format.");
            }
        }
        //打印检查
        for(Production production:productions){
            System.out.println(production.toString());
        }
    }

    public static void main(String[] args) {
        analysis();
    }
}