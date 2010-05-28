/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of ub0rlib.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.lib.apis;

import java.io.InputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import de.ub0r.android.lib.Log;

/**
 * Helper class to set/unset background for api5 systems.
 * 
 * @author flx
 */
public final class ContactsWrapper5 extends ContactsWrapper {
	/** Tag for output. */
	private static final String TAG = "cw5";

	/** Projection for persons query, filter. */
	private static final String[] PROJECTION_FILTER = // .
	new String[] { ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY, // 0
			ContactsContract.Data.DISPLAY_NAME, // 1
			ContactsContract.CommonDataKinds.Phone.NUMBER, // 2
			ContactsContract.CommonDataKinds.Phone.TYPE // 3
	};

	/** Projection for persons query, show. */
	private static final String[] PROJECTION_CONTENT = // .
	new String[] { BaseColumns._ID, // 0
			ContactsContract.Data.DISPLAY_NAME, // 1
			ContactsContract.CommonDataKinds.Phone.NUMBER, // 2
			ContactsContract.CommonDataKinds.Phone.TYPE // 3
	};

	/** SQL to select mobile numbers only. */
	private static final String MOBILES_ONLY = ") AND ("
			+ ContactsContract.CommonDataKinds.Phone.TYPE + " = "
			+ ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE + ")";

	/** Sort Order. */
	private static final String SORT_ORDER = // .
	ContactsContract.CommonDataKinds.Phone.STARRED + " DESC, "
			+ ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED
			+ " DESC, " + ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
			+ " ASC, " + ContactsContract.CommonDataKinds.Phone.TYPE;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bitmap loadContactPhoto(final Context context, // .
			final String contactId) {
		if (contactId == null || contactId.length() == 0) {
			return null;
		}
		try {
			final ContentResolver cr = context.getContentResolver();
			InputStream is = Contacts.openContactPhotoInputStream(cr, this
					.getContactUri(cr, contactId));
			if (is == null) {
				return null;
			}
			return BitmapFactory.decodeStream(is);
		} catch (Exception e) {
			Log.e(TAG, "error getting photo: " + contactId, e);
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri getContactUri(final ContentResolver cr, final String id) {
		return Contacts.lookupContact(cr, Uri.withAppendedPath(
				Contacts.CONTENT_LOOKUP_URI, id));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor getContact(final ContentResolver cr, // .
			final String number) {
		if (number == null || number.length() == 0) {
			return null;
		}
		Uri uri = Uri.withAppendedPath(
				ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
				number);
		// FIXME: this is broken in android os; issue #8255
		Log.d(TAG, "query: " + uri);
		Cursor c = cr.query(uri, PROJECTION_FILTER, null, null, null);
		if (c != null && c.moveToFirst()) {
			return c;
		}
		// Fallback to API3
		c = new ContactsWrapper3().getContact(cr, number);
		if (c != null && c.moveToFirst()) {
			// get orig API5 cursor for the real number
			final String where = PROJECTION_FILTER[FILTER_INDEX_NUMBER]
					+ " = '" + c.getString(FILTER_INDEX_NUMBER) + "'";
			Log.d(TAG, "query: " + Phone.CONTENT_URI + " # " + where);
			Cursor c0 = cr.query(Phone.CONTENT_URI, PROJECTION_FILTER, where,
					null, null);
			if (c0 != null && c0.moveToFirst()) {
				Log.d(TAG, "id: " + c0.getString(FILTER_INDEX_ID));
				Log.d(TAG, "name: " + c0.getString(FILTER_INDEX_NAME));
				Log.d(TAG, "number: " + c0.getString(FILTER_INDEX_NUMBER));
				return c0;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor getContact(final ContentResolver cr, // .
			final Uri uri) {
		// FIXME: this is broken in android os; issue #8255
		Log.d(TAG, "query: " + uri);
		Cursor c = cr.query(uri, PROJECTION_FILTER, null, null, null);
		if (c != null && c.moveToFirst()) {
			return c;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri getContentUri() {
		return ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getContentProjection() {
		return PROJECTION_CONTENT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMobilesOnlyString() {
		return MOBILES_ONLY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getContentSort() {
		return SORT_ORDER;
	}

	@Override
	public String getContentWhere(final String filter) {
		String f = DatabaseUtils.sqlEscapeString('%' + filter.toString() + '%');
		StringBuilder s = new StringBuilder();
		s.append("(" + ContactsContract.Data.DISPLAY_NAME + " LIKE ");
		s.append(f);
		s.append(") OR (" + ContactsContract.CommonDataKinds.Phone.DATA1
				+ " LIKE ");
		s.append(f);
		s.append(")");
		return s.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Intent getPickPhoneIntent() {
		final Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
		return i;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Intent getInsertPickIntent(final String address) {
		final Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
		i.putExtra(ContactsContract.Intents.Insert.PHONE, address);
		i.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
				ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
		return i;
	}

}
