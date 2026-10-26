package tw.edu.ctust.ncdphone;
public class RulesTest {
    static int checks;
    static void check(boolean ok){checks++;if(!ok)throw new AssertionError("Check "+checks+" failed");}
    public static void main(String[] args){
        check(Rules.samePhone("0912-345-678","+886912345678"));
        check(Rules.samePhone("02 1234 5678","00886212345678"));
        check(!Rules.samePhone("",""));check(!Rules.samePhone("0912345678","0992345678"));
        check(!Rules.samePhone("12345678","9912345678"));
        check(Rules.validPhone("+886 912-345-678"));check(!Rules.validPhone("*21*0912345678#"));check(!Rules.validPhone("abc"));
        check(Rules.pageCount(0)==1);check(Rules.pageCount(4)==1);check(Rules.pageCount(5)==2);check(Rules.pageCount(9)==3);
        check(Rules.contactIndex(1,0,5)==4);check(Rules.contactIndex(1,1,5)==-1);
        System.out.println(checks+" policy checks passed");
    }
}
