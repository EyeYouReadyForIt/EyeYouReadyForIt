package io.github.boogiemonster1o1.eyeyoureadyforit.data;

public class Statistic {

    private final int correctAnswers;
    private final int wrongAnswers;
    private final int hintUses;

    public Statistic(int correctAnswers, int wrongAnswers, int hintUses) {

        // i had to use all my self control to not make it throw 'thoo'
        if(correctAnswers < 0 || wrongAnswers < 0 || hintUses < 0) throw new IllegalArgumentException("Statistic values cannot be negative");

        this.correctAnswers = correctAnswers;
        this.wrongAnswers = wrongAnswers;
        this.hintUses = hintUses;
    }

    public Statistic() {
        this.correctAnswers = 0;
        this.wrongAnswers = 0;
        this.hintUses = 0;
    }

    public int getCorrectAnswers() { return correctAnswers; }

    public int getWrongAnswers() { return wrongAnswers; }

    public int getHintUses() { return hintUses; }

    public Statistic add(Statistic toAdd) {
        return new Statistic(
                this.correctAnswers + toAdd.getCorrectAnswers(),
                this.wrongAnswers + toAdd.getWrongAnswers(),
                this.hintUses + toAdd.getHintUses()
        );
    }

    public Statistic subtract(Statistic toSubtract) {
        return new Statistic(
                this.correctAnswers - toSubtract.getCorrectAnswers(),
                this.wrongAnswers - toSubtract.getWrongAnswers(),
                this.hintUses - toSubtract.getHintUses()
        );
    }
}
