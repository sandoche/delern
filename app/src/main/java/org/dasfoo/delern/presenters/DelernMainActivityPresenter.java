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

package org.dasfoo.delern.presenters;

import android.support.annotation.Nullable;

import org.dasfoo.delern.models.Deck;
import org.dasfoo.delern.models.DeckType;
import org.dasfoo.delern.models.User;
import org.dasfoo.delern.models.listeners.AbstractDataAvailableListener;
import org.dasfoo.delern.models.listeners.OnOperationCompleteListener;
import org.dasfoo.delern.views.IDelernMainView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Presenter for DelernMainActivity. It implements OnDeckViewHolderClick to handle
 * user clicks. Class calls activity callbacks to show changed user data.
 */
public class DelernMainActivityPresenter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelernMainActivityPresenter.class);

    private final IDelernMainView mDelernMainView;
    private AbstractDataAvailableListener<Long> mUserHasDecksListener;
    private AbstractDataAvailableListener<User> mAbstractDataAvailableListener;
    private User mUser;

    /**
     * Constructor for DelernMainActivityPresenter. It gets DelernMainActivity view to perform
     * callbacks to Activity.
     *
     * @param delernMainView IDelernMainView for performing callbacks.
     */
    public DelernMainActivityPresenter(final IDelernMainView delernMainView) {
        this.mDelernMainView = delernMainView;
    }

    /**
     * Called from DelernMainActivity.onCreate(). Method checks whether user is signed in.
     * If not, it notifies DelernMainActivity that onCreate is not performed.
     * Method checks whether user has decks.
     *
     * @param user current user
     * @return whether onCreate() was performed or not.
     */
    public boolean onCreate(final User user) {
        if (user == null || !user.exists()) {
            LOGGER.debug("User is not Signed In");
            mDelernMainView.signIn();
            return false;
        }
        mUser = user;
        mUserHasDecksListener = new AbstractDataAvailableListener<Long>() {

            @Override
            public void onData(@Nullable final Long isUserHasDecks) {
                mDelernMainView.showProgressBar(false);
                if (isUserHasDecks == null || isUserHasDecks != 1) {
                    mDelernMainView.noDecksMessage(true);
                } else {
                    mDelernMainView.noDecksMessage(false);
                }
            }
        };
        return true;
    }

    /**
     * Method is called in DelernMainActivity.onStart. It checks
     * whether user has decks or not.
     */
    public void onStart() {
        Deck.fetchCount(mUser.getChildReference(Deck.class).limitToFirst(1), mUserHasDecksListener);
    }

    /**
     * Method is called in onStop in DelernMainActivity to release used resources.
     */
    public void onStop() {
        cleanup();
    }

    /**
     * Method renames deck.
     *
     * @param deck deck to rename.
     * @param newName new name for deck.
     */
    public void renameDeck(final Deck deck, final String newName) {
        deck.setName(newName);
        deck.save(null);
    }

    /**
     * Method deletes deck.
     *
     * @param deck deck to delete.
     */
    public void deleteDeck(final Deck deck) {
        deck.delete();
    }

    /**
     * Method changes type of deck.
     *
     * @param deck deck type of which to change.
     * @param deckType new type of deck.
     */
    public void changeDeckType(final Deck deck, final DeckType deckType) {
        deck.setDeckType(deckType.name());
        deck.save(null);
    }


    /**
     * Cleanup listeners and release resources.
     */
    public void cleanup() {
        mUserHasDecksListener.cleanup();
        mAbstractDataAvailableListener.cleanup();
    }

    /**
     * Method creates new deck. It gets as parameter name of deck.
     *
     * @param deckName name of deck
     */
    public void createNewDeck(final String deckName) {
        final Deck newDeck = new Deck(mUser);
        newDeck.setName(deckName);
        newDeck.setDeckType(DeckType.BASIC.name());
        newDeck.setAccepted(true);
        newDeck.create(new OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                mDelernMainView.addCardsToDeck(newDeck);
            }
        });
    }

    /**
     * Gets user data from FB Database. If user doesn't exist, calls sign in.
     * Otherwise calls callback method to update user profile info.
     */
    public void getUserInfo() {
        mAbstractDataAvailableListener = new AbstractDataAvailableListener<User>() {
            @Override
            public void onData(@Nullable final User user) {
                LOGGER.debug("Check if user null");
                if (user == null) {
                    LOGGER.debug("Starting sign in");
                    mDelernMainView.signIn();
                } else {
                    mUser = user;
                    mDelernMainView.updateUserProfileInfo(user);
                }
            }
        };
        mUser.watch(mAbstractDataAvailableListener, User.class);
    }

    /**
     * Getter for user.
     *
     * @return user.
     */
    public User getUser() {
        return mUser;
    }
}
