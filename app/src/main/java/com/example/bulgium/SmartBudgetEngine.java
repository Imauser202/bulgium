package com.example.bulgium;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SmartBudgetEngine {

    public interface AdviceCallback {
        void onAdvice(String advice);
    }

    public enum BudgetStrategy {
        BALANCED_50_30_20("50/30/20 Rule", "Allocates 50% to Needs, 30% to Wants, and 20% to Savings."),
        AGGRESSIVE_SAVER("Aggressive Saving", "Prioritizes savings (40%) and minimizes wants."),
        STUDENT_PRIORITY("Student Life", "Prioritizes school expenses and food."),
        MINIMALIST("Minimalist", "Focuses only on essential needs and maximum savings.");

        public final String name;
        public final String description;

        BudgetStrategy(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    public static void getAdvice(List<Transaction> history, AdviceCallback callback) {
        if (history.isEmpty()) {
            callback.onAdvice("Your financial journey starts with a single transaction. Add one to see the magic! ✨");
            return;
        }

        double totalIncome = 0;
        double totalExpenses = 0;
        Map<String, Double> categoryTotals = new HashMap<>();

        for (Transaction t : history) {
            if (t.isIncome()) {
                totalIncome += t.getAmount();
            } else {
                totalExpenses += t.getAmount();
                categoryTotals.put(t.getCategory(), categoryTotals.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
            }
        }

        List<String> insights = new ArrayList<>();
        
        // Savings Rate Logic
        if (totalIncome > 0) {
            double savingsRate = ((totalIncome - totalExpenses) / totalIncome) * 100;
            if (savingsRate >= 30) {
                insights.add("You're a financial superstar! A " + (int)savingsRate + "% savings rate is incredible. 🌟");
            } else if (savingsRate >= 20) {
                insights.add("Solid work! You're hitting the gold standard 20% savings rate. 💰");
            } else if (savingsRate > 0) {
                insights.add("You're saving " + (int)savingsRate + "%. Try to squeeze a bit more into your piggy bank! 🐷");
            } else {
                insights.add("Warning: Your expenses are outpacing your income. Time for a quick budget audit! ⚠️");
            }
        }

        // Top Category Logic
        String topCategory = "";
        double maxAmount = 0;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue() > maxAmount) {
                maxAmount = entry.getValue();
                topCategory = entry.getKey();
            }
        }

        if (!topCategory.isEmpty()) {
            String cat = topCategory.toLowerCase();
            if (cat.contains("food")) {
                insights.add("Your inner foodie is winning! Maybe try meal prepping next week? 🍱");
            } else if (cat.contains("leisure") || cat.contains("game")) {
                insights.add("Treat yourself, but watch that leisure spend. Balance is key! 🎮");
            } else if (cat.contains("transport")) {
                insights.add("Commuting is taking a big slice. Any chance for carpooling? 🚗");
            } else if (cat.contains("school")) {
                insights.add("Investing in your education is the best investment. Keep it up! 📚");
            } else {
                insights.add("Your biggest spend is on " + topCategory + ". Is it worth the value? 🤔");
            }
        }

        // Random Tip
        String[] generalTips = {
            "Tip: Small daily savings can lead to big monthly gains!",
            "Pro-tip: Review your subscriptions. Are you actually using them all?",
            "Remember: A budget is telling your money where to go instead of wondering where it went.",
            "Hack: Wait 24 hours before making any non-essential purchase."
        };
        insights.add(generalTips[new Random().nextInt(generalTips.length)]);

        // Combine insights
        StringBuilder finalAdvice = new StringBuilder();
        for (int i = 0; i < insights.size(); i++) {
            finalAdvice.append(insights.get(i));
            if (i < insights.size() - 1) finalAdvice.append(" ");
        }

        callback.onAdvice(finalAdvice.toString());
    }

    public static Map<String, Double> getBudgetSuggestions(double incomeAmount, List<Transaction> transactions) {
        // We'll default to a 'Balanced' strategy but can be expanded
        return getBudgetSuggestions(incomeAmount, transactions, BudgetStrategy.BALANCED_50_30_20);
    }

    public static Map<String, Double> getBudgetSuggestions(double incomeAmount, List<Transaction> transactions, BudgetStrategy strategy) {
        Map<String, Double> suggestions = new HashMap<>();
        if (incomeAmount <= 0) return suggestions;

        double needsPercent, wantsPercent, savingsPercent;

        switch (strategy) {
            case AGGRESSIVE_SAVER:
                needsPercent = 0.50;
                wantsPercent = 0.10;
                savingsPercent = 0.40;
                break;
            case STUDENT_PRIORITY:
                needsPercent = 0.70; // Including school and food
                wantsPercent = 0.20;
                savingsPercent = 0.10;
                break;
            case MINIMALIST:
                needsPercent = 0.40;
                wantsPercent = 0.05;
                savingsPercent = 0.55;
                break;
            case BALANCED_50_30_20:
            default:
                needsPercent = 0.50;
                wantsPercent = 0.30;
                savingsPercent = 0.20;
                break;
        }

        double needsBudget = incomeAmount * needsPercent;
        double wantsBudget = incomeAmount * wantsPercent;
        double savingsBudget = incomeAmount * savingsPercent;

        // Map categories to groups
        // Needs: Food, Transport, School
        // Wants: Leisure, Others
        
        suggestions.put("Needs (Food, Transpo, School)", needsBudget);
        suggestions.put("Wants (Leisure, etc.)", wantsBudget);
        suggestions.put("Savings Goal", savingsBudget);

        return suggestions;
    }
}
