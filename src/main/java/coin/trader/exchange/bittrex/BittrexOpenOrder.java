package coin.trader.exchange.bittrex;

import java.io.Serializable;

import coin.trader.exchange.OpenOrder;

public class BittrexOpenOrder implements OpenOrder, Serializable {
	private static final long serialVersionUID = -8095674158737613191L;

	String Uuid;
	String OrderUuid;
	String Exchange;
	String OrderType;
	double Quantity;
	double QuantityRemaining;
	double Limit;
	double CommissionPaid;
	double Price;
	double PricePerUnit;
	String Opened;
	String Closed;
	boolean CancelInitialed;
	boolean ImmediateOrCancel;
	boolean IsConditional;
	String Condition;
	String ConditionTarget;

	@Override
	public boolean isClosed() {
		return this.Closed != null;
	}

	@Override
	public boolean isPartiallyFilled() {
		return this.Quantity != this.QuantityRemaining;
	}

	public static class BittrexOpenOrderResponse implements Serializable {
		private static final long serialVersionUID = 3482289112369786015L;

		String message = "";
		boolean success = false;

		BittrexOpenOrderAccount result = null;

		public static class BittrexOpenOrderAccount implements Serializable {
			private static final long serialVersionUID = -6070408948176696882L;

			String AccountId;
			String OrderUuid;
			String Exchange;
			String Type;
			double Quantity;
			double QuantityRemaining;
			double Limit;
			double Reserved;
			double ReserveRemaining;
			double CommissionReserved;
			double CommissionReserveRemaining;
			double CommissionPaid;
			double Price;
			double PricePerUnit;
			String Opened;
			String Closed;
			boolean IsOpen;
			String Sentinel;
			boolean CancelInitiated;
			boolean ImmediateOrCancel;
			boolean IsConditional;
			String Condition;
			String ConditionTarget;
		}

		BittrexOpenOrder toOpenOrder() {
			final BittrexOpenOrder openOrder = new BittrexOpenOrder();
			openOrder.Uuid = this.result.OrderUuid;
			openOrder.Exchange = this.result.Exchange;
			openOrder.Quantity = this.result.Quantity;
			openOrder.QuantityRemaining = this.result.QuantityRemaining;
			openOrder.Limit = this.result.Limit;
			openOrder.Price = this.result.Price;
			openOrder.PricePerUnit = this.result.PricePerUnit;
			openOrder.Opened = this.result.Opened;
			openOrder.Closed = this.result.Closed;
			openOrder.CancelInitialed = this.result.CancelInitiated;
			openOrder.ImmediateOrCancel = this.result.ImmediateOrCancel;
			openOrder.IsConditional = this.result.IsConditional;
			openOrder.Condition = this.result.Condition;
			openOrder.ConditionTarget = this.result.ConditionTarget;
			return openOrder;
		}
	}
}
