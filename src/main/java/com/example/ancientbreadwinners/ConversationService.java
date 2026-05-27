package com.example.ancientbreadwinners;

import java.util.List;

class ConversationService {
    private final HelloApplication app;

    ConversationService(HelloApplication app) {
        this.app = app;
    }

    void checkFarmerTalking(List<Farmer> farmers, long now) {
        for (int i = 0; i < farmers.size(); i++) {
            Farmer a = farmers.get(i);
            if (!canStartConversation(a, now)) continue;
            boolean startedConversation = false;
            for (int j = i + 1; j < farmers.size(); j++) {
                Farmer b = farmers.get(j);
                if (!canStartConversation(b, now) || !areCloseForTalk(a, b)) continue;

                for (int k = j + 1; k < farmers.size(); k++) {
                    Farmer c = farmers.get(k);
                    if (!canStartConversation(c, now)) continue;
                    if (!areCloseForTalk(a, c) || !areCloseForTalk(b, c)) continue;

                    a.speak(b, c);
                    beginConversation(FarmerState.TALKING_TRIPLE, now, a, b, c);
                    startedConversation = true;
                    break;
                }

                if (startedConversation) break;

                a.speak(b);
                beginConversation(FarmerState.TALKING, now, a, b);
                startedConversation = true;
                break;
            }
        }
    }

    private boolean canStartConversation(Farmer farmer, long now) {
        FarmerState state = farmer.getState();
        if (state == FarmerState.TALKING || state == FarmerState.TALKING_TRIPLE
                || state == FarmerState.RESTING_AT_CHURCH
                || state == FarmerState.WALKING_TO_CHURCH_WITH_MONEY) {
            return false;
        }
        if (farmer.getMotivation() > 65) return false;
        return (now - farmer.getLastSpokeNano()) > HelloApplication.TALK_COOLDOWN_NS;
    }

    private boolean areCloseForTalk(Farmer a, Farmer b) {
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY()) < HelloApplication.TALK_DISTANCE;
    }

    private void beginConversation(FarmerState conversationState, long now, Farmer... participants) {
        long end = now + HelloApplication.TALK_DURATION_NS;
        for (Farmer participant : participants) {
            participant.setStateBeforeTalk(participant.getState());
            participant.setTalkTimerEnd(end);
            participant.setLastSpokeNano(now);
            participant.setState(conversationState);
        }
    }
}

