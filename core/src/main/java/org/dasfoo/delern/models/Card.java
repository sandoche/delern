/*
 * Copyright (C) 2017 Katarina Sheremet
 * This file is part of Delern.
 *
 * Delern is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Delern is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with  Delern.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.dasfoo.delern.models;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;

import org.dasfoo.delern.models.helpers.MultiWrite;
import org.dasfoo.delern.util.MarkdownParser;

import io.reactivex.Completable;
import io.reactivex.annotations.Nullable;

/**
 * Created by katarina on 10/4/16.
 * Model class for card.
 */
@SuppressWarnings({"checkstyle:MemberName", "checkstyle:HiddenField"})
public class Card extends Model {

    /**
     * Answer on card if user knows it.
     */
    private static final String KNOW_CARD = "Y";

    /**
     * Answer on card if user doesn't know it.
     */
    private static final String DO_NOT_KNOW_CARD = "N";

    private String back;
    private String front;

    /**
     * An empty constructor is required for Firebase deserialization.
     */
    private Card() {
        super(null, null);
    }

    /**
     * Create a card object with Deck as a parent.
     *
     * @param parent Deck which this card belongs to.
     */
    public Card(final Deck parent) {
        super(parent, null);
    }

    /**
     * Create a card object with ScheduledCard as a parent.
     *
     * @param parent ScheduledCard associated with this card.
     */
    public Card(final ScheduledCard parent) {
        super(parent, null);
    }

    /**
     * Set the key for the card and associated ScheduledCard (for saving it later).
     *
     * @param key value of the key (usually a fairly random string).
     */
    @Exclude
    @Override
    public void setKey(final String key) {
        super.setKey(key);
        ScheduledCard sc = getScheduledCard();
        if (sc != null && !sc.exists()) {
            sc.setKey(key);
        }
    }

    /**
     * Get the Deck this Card belongs to, directly or via associated ScheduledCard.
     *
     * @return Model parent casted to Deck (if set).
     */
    @Exclude
    public Deck getDeck() {
        Model parent = getParent();
        if (parent instanceof Deck) {
            return (Deck) parent;
        }
        return ((ScheduledCard) parent).getDeck();
    }

    /**
     * Method for adding card to FB.
     *
     * @return FirebaseTaskAdapter for the write operation.
     */
    @Exclude
    public Completable create() {
        ScheduledCard scheduledCard = new ScheduledCard(getDeck());
        scheduledCard.setLevel(Level.L0.name());
        scheduledCard.setRepeatAt(System.currentTimeMillis());
        setParent(scheduledCard);

        return new MultiWrite()
                .save(this)
                .save(scheduledCard)
                .write();
    }

    /**
     * Get the ScheduledCard this Card is associated with.
     *
     * @return Model parent casted to ScheduledCard (if set).
     */
    @Exclude
    @Nullable
    public ScheduledCard getScheduledCard() {
        Model parent = getParent();
        if (parent instanceof ScheduledCard) {
            return (ScheduledCard) parent;
        }
        return null;
    }

    /**
     * Update the ScheduledCard associated with this card and add a View (saves to the database).
     *
     * @param knows whether the user replied with "I know" to the card.
     * @return FirebaseTaskAdapter for the write operation.
     */
    @Exclude
    public Completable answer(final boolean knows) {
        String newCardLevel;
        String reply;
        ScheduledCard sc = getScheduledCard();
        if (knows) {
            newCardLevel = Level.getNextLevel(sc.getLevel());
            reply = KNOW_CARD;
        } else {
            newCardLevel = Level.L0.name();
            reply = DO_NOT_KNOW_CARD;
        }

        View v = new View(this);
        v.setLevelBefore(sc.getLevel());
        v.setReply(reply);

        sc.setLevel(newCardLevel);
        sc.setRepeatAt(RepetitionIntervals.getInstance()
                .getNextTimeToRepeat(newCardLevel));

        return new MultiWrite()
                .save(this)
                .save(v)
                .save(sc)
                .write();
    }

    /**
     * Removes the Card, ScheduledCard and Views from the database.
     *
     * @return FirebaseTaskAdapter for the delete operation.
     */
    @Exclude
    public Completable delete() {
        return new MultiWrite()
                .delete(this)
                .delete(getDeck().getChildReference(ScheduledCard.class, getKey()))
                .delete(getChildReference(View.class, getKey()))
                .write();
    }

    /**
     * Getter for back side of card.
     *
     * @return back of card.
     */
    public String getBack() {
        return back;
    }

    /**
     * Non-caching converter of Markdown to HTML for back side of card.
     *
     * @return back of card in HTML.
     */
    @Exclude
    public String getBackHtml() {
        return MarkdownParser.INSTANCE.toHtml(back);
    }

    /**
     * Setter for back side of card.
     *
     * @param backSide back of card.
     */
    public void setBack(final String backSide) {
        this.back = backSide;
    }

    /**
     * Getter for front side of card.
     *
     * @return front side of card
     */
    public String getFront() {
        return front;
    }

    /**
     * Non-caching converter of Markdown to HTML for front side of card.
     *
     * @return front of card in HTML.
     */
    @Exclude
    public String getFrontHtml() {
        return MarkdownParser.INSTANCE.toHtml(front);
    }

    /**
     * Setter for front side of card.
     *
     * @param frontSide front side of card.
     */
    public void setFront(final String frontSide) {
        this.front = frontSide;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Card{" + super.toString() +
                ", back='" + back + '\'' +
                ", front='" + front + '\'' +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Exclude
    @Override
    public <T> DatabaseReference getChildReference(final Class<T> childClass) {
        if (childClass == View.class) {
            return getDeck().getChildReference(View.class, getKey());
        }
        return super.getChildReference(childClass);
    }
}
