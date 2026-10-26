package tw.edu.ctust.ncdphone;
public class RulesTest {
    static int checks;
    static void check(boolean ok){checks++;if(!ok)throw new AssertionError("Check "+checks+" failed");}
    public static void main(String[] args){
        FlipDetector flip=new FlipDetector();check(!flip.update(-9,0));check(!flip.update(9,200));
        check(!flip.update(-9,1000));check(flip.update(0,1800));check(!flip.update(9,1900));
        check(!flip.update(-9,2000));check(flip.update(9,70000));
        check(!flip.update(-9,71000));flip.reset();check(!flip.update(9,72000));
        check(Rules.isPageSwipe(-180,20,72));check(Rules.isPageSwipe(150,-30,72));check(!Rules.isPageSwipe(30,3,72));check(!Rules.isPageSwipe(80,100,72));
        check(Rules.sameLineName("Cathy tu","Cathy Tu"));check(!Rules.sameLineName("Cathy","Cathy Tu"));check(!Rules.sameLineName(""," "));
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
