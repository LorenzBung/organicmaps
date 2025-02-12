package app.organicmaps.car.screens;

import static java.util.Objects.requireNonNull;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.ForegroundCarColorSpan;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MapTemplate;
import androidx.core.graphics.drawable.IconCompat;

import app.organicmaps.R;
import app.organicmaps.bookmarks.data.BookmarkCategory;
import app.organicmaps.bookmarks.data.BookmarkInfo;
import app.organicmaps.bookmarks.data.BookmarkManager;
import app.organicmaps.car.SurfaceRenderer;
import app.organicmaps.car.screens.base.BaseMapScreen;
import app.organicmaps.car.util.Colors;
import app.organicmaps.car.util.RoutingHelpers;
import app.organicmaps.car.util.UiHelpers;
import app.organicmaps.location.LocationHelper;
import app.organicmaps.util.Distance;
import app.organicmaps.util.Graphics;

import java.util.ArrayList;
import java.util.List;

public class BookmarksScreen extends BaseMapScreen
{
  private final int MAX_CATEGORIES_SIZE;

  @Nullable
  private BookmarkCategory mBookmarkCategory;

  public BookmarksScreen(@NonNull CarContext carContext, @NonNull SurfaceRenderer surfaceRenderer)
  {
    super(carContext, surfaceRenderer);
    final ConstraintManager constraintManager = getCarContext().getCarService(ConstraintManager.class);
    MAX_CATEGORIES_SIZE = constraintManager.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST);
  }

  private BookmarksScreen(@NonNull CarContext carContext, @NonNull SurfaceRenderer surfaceRenderer, @NonNull BookmarkCategory bookmarkCategory)
  {
    this(carContext, surfaceRenderer);
    mBookmarkCategory = bookmarkCategory;
  }

  @NonNull
  @Override
  public Template onGetTemplate()
  {
    final MapTemplate.Builder builder = new MapTemplate.Builder();
    builder.setHeader(createHeader());
    builder.setMapController(UiHelpers.createMapController(getCarContext(), getSurfaceRenderer()));
    builder.setItemList(mBookmarkCategory == null ? createBookmarkCategoriesList() : createBookmarksList());
    return builder.build();
  }

  @NonNull
  private Header createHeader()
  {
    final Header.Builder builder = new Header.Builder();
    builder.setStartHeaderAction(Action.BACK);
    builder.setTitle(mBookmarkCategory == null ? getCarContext().getString(R.string.bookmarks) : mBookmarkCategory.getName());
    return builder.build();
  }

  @NonNull
  private ItemList createBookmarkCategoriesList()
  {
    final List<BookmarkCategory> bookmarkCategories = getBookmarks();
    final int categoriesSize = Math.min(bookmarkCategories.size(), MAX_CATEGORIES_SIZE);

    ItemList.Builder builder = new ItemList.Builder();
    for (int i = 0; i < categoriesSize; ++i)
    {
      final BookmarkCategory bookmarkCategory = bookmarkCategories.get(i);

      Row.Builder itemBuilder = new Row.Builder();
      itemBuilder.setTitle(bookmarkCategory.getName());
      itemBuilder.addText(bookmarkCategory.getDescription());
      itemBuilder.setOnClickListener(() -> getScreenManager().push(new BookmarksScreen(getCarContext(), getSurfaceRenderer(), bookmarkCategory)));
      itemBuilder.setBrowsable(true);
      builder.addItem(itemBuilder.build());
    }
    return builder.build();
  }

  @NonNull
  private ItemList createBookmarksList()
  {
    final long bookmarkCategoryId = requireNonNull(mBookmarkCategory).getId();
    final int bookmarkCategoriesSize = Math.min(mBookmarkCategory.getBookmarksCount(), MAX_CATEGORIES_SIZE);

    ItemList.Builder builder = new ItemList.Builder();
    for (int i = 0; i < bookmarkCategoriesSize; ++i)
    {
      final long bookmarkId = BookmarkManager.INSTANCE.getBookmarkIdByPosition(bookmarkCategoryId, i);
      final BookmarkInfo bookmarkInfo = new BookmarkInfo(bookmarkCategoryId, bookmarkId);

      final Row.Builder itemBuilder = new Row.Builder();
      itemBuilder.setTitle(bookmarkInfo.getName());
      if (!bookmarkInfo.getAddress().isEmpty())
        itemBuilder.addText(bookmarkInfo.getAddress());
      final CharSequence description = getDescription(bookmarkInfo);
      if (description.length() != 0)
        itemBuilder.addText(description);
      final Drawable icon = Graphics.drawCircleAndImage(bookmarkInfo.getIcon().argb(),
          R.dimen.track_circle_size,
          bookmarkInfo.getIcon().getResId(),
          R.dimen.bookmark_icon_size,
          getCarContext());
      itemBuilder.setImage(new CarIcon.Builder(IconCompat.createWithBitmap(Graphics.drawableToBitmap(icon))).build());
      itemBuilder.setOnClickListener(() -> BookmarkManager.INSTANCE.showBookmarkOnMap(bookmarkId));
      builder.addItem(itemBuilder.build());
    }
    return builder.build();
  }

  @NonNull
  private CharSequence getDescription(final BookmarkInfo bookmark)
  {
    final SpannableStringBuilder result = new SpannableStringBuilder(" ");
    final Location loc = LocationHelper.from(getCarContext()).getSavedLocation();
    if (loc != null)
    {
      final Distance distance = bookmark.getDistance(loc.getLatitude(), loc.getLongitude(), 0.0);
      result.setSpan(DistanceSpan.create(RoutingHelpers.createDistance(distance)), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      result.setSpan(ForegroundCarColorSpan.create(Colors.DISTANCE), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    if (loc != null && !bookmark.getFeatureType().isEmpty())
    {
      result.append(" • ");
      result.append(bookmark.getFeatureType());
    }

    return result;
  }

  @NonNull
  private static List<BookmarkCategory> getBookmarks()
  {
    final List<BookmarkCategory> bookmarkCategories = new ArrayList<>(BookmarkManager.INSTANCE.getCategories());

    final List<BookmarkCategory> toRemove = new ArrayList<>();
    for (final BookmarkCategory bookmarkCategory : bookmarkCategories)
    {
      if (bookmarkCategory.getBookmarksCount() == 0 || !bookmarkCategory.isVisible())
        toRemove.add(bookmarkCategory);
    }
    bookmarkCategories.removeAll(toRemove);

    return bookmarkCategories;
  }
}
