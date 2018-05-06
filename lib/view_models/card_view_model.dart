import 'package:meta/meta.dart';

import '../models/card.dart';
import 'base/activatable.dart';
import 'base/proxy_keyed_list.dart';
import 'base/view_models_list.dart';

class CardViewModel implements ViewModel {
  String get key => _card?.key;
  Card get card => _card;
  String get front => _card?.front;
  String get back => _card?.back;

  Card _card;

  CardViewModel(this._card);

  @override
  CardViewModel updateWith(CardViewModel value) => value;

  @override
  @mustCallSuper
  void activate() {}

  @override
  @mustCallSuper
  void deactivate() {}

  @override
  String toString() {
    return '#$key $front $back';
  }
}

class CardsViewModel implements Activatable {
  final String deckId;

  ViewModelsList<CardViewModel> _cardViewModels;
  ProxyKeyedList<CardViewModel> _cardsProxy;

  ProxyKeyedList<CardViewModel> get cards =>
      _cardsProxy ??= new ProxyKeyedList(_cardViewModels);

  CardsViewModel(this.deckId) {
    _cardViewModels = new ViewModelsList<CardViewModel>(() => Card
        .getCards(deckId)
        .map((cardEvent) => cardEvent.map((card) => new CardViewModel(card))));
  }

  @override
  @mustCallSuper
  void deactivate() => _cardViewModels.deactivate();

  @override
  @mustCallSuper
  void activate() {
    deactivate();
    _cardViewModels.activate();
  }

  @mustCallSuper
  void dispose() {
    deactivate();
    _cardsProxy?.dispose();
  }
}
