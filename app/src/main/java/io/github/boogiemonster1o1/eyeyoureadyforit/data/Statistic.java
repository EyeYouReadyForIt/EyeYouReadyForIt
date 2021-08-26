package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import java.util.Objects;

public class Statistic {

    private int correctAnswers;
    private int wrongAnswers;
    private int hintUses;

    public Statistic(int correctAnswers, int wrongAnswers, int hintUses) {

        // i had to use all my self control to not make it throw 'thoo'
        if(correctAnswers < 0 || wrongAnswers < 0 || hintUses < 0) throw new IllegalArgumentException("Statistic values cannot be negative");

        this.correctAnswers = correctAnswers;
        this.wrongAnswers = wrongAnswers;
        this.hintUses = hintUses;
    }

    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }

    public int getWrongAnswers() { return wrongAnswers; }
    public void setWrongAnswers(int wrongAnswers) { this.wrongAnswers = wrongAnswers; }

    public int getHintUses() { return hintUses; }
    public void setHintUses(int hintUses) { this.hintUses = hintUses; }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Statistic statistic = (Statistic) o;
        return correctAnswers == statistic.correctAnswers && wrongAnswers == statistic.wrongAnswers && hintUses == statistic.hintUses;
    }

    @Override
    public int hashCode() {
        return Objects.hash(correctAnswers, wrongAnswers, hintUses);
    }
}
