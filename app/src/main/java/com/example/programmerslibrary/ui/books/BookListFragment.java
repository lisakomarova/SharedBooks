package com.example.programmerslibrary.ui.books;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.programmerslibrary.DataBase.MyAPIHelper;
import com.example.programmerslibrary.Enumerations.BookStatus;
import com.example.programmerslibrary.MainActivity;
import com.example.programmerslibrary.R;
import com.example.programmerslibrary.SignUpActivity;
import com.example.programmerslibrary.Utils.BookAdapter;
import com.example.programmerslibrary.Utils.RecyclerTouchListener;
import com.example.programmerslibrary.models.Book;
import com.example.programmerslibrary.models.Loan;
import com.example.programmerslibrary.models.Reader;
import com.example.programmerslibrary.ui.loan.IssueBookFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class BookListFragment extends Fragment {

    MyAPIHelper db;
    String user_id;
    FragmentManager fragmentManager;
    private ArrayList<Book> bookList = new ArrayList<>();
    private BookAdapter mAdapter;
    private RecyclerView recyclerView;
    private TextView noNotesView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        fragmentManager = getActivity().getSupportFragmentManager();
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
        toggleEmptyNotes();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_book_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        noNotesView = view.findViewById(R.id.empty_notes_view);

        registerForContextMenu(recyclerView);


        user_id = MainActivity.getUserID();
        db = MainActivity.getDb();

        bookList.addAll(db.getAllBooks(user_id));

        FloatingActionButton fab = (FloatingActionButton)view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create new fragment and transaction
                Fragment newFragment = new AddBookFragment();
                Bundle args = new Bundle();
                args.putString("user", user_id);

                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

                newFragment.setArguments(args);
                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack if needed
                transaction.replace(R.id.nav_host_fragment, newFragment);
                transaction.addToBackStack(null);

                // Commit the transaction
                transaction.commit();
            }
        });

        mAdapter = new BookAdapter(getContext(), bookList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        toggleEmptyNotes();

        /**
         * On long press on RecyclerView item, open alert dialog
         * with options to choose
         * Edit and Delete
         * */
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(),
                recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, final int position) {

            }

            @Override
            public void onLongClick(View view, int position) {
                showActionsDialog(position);
            }
        }));

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);

        return view;
    }


    /**
     * Deleting book from SQLite and removing the
     * item from the list by its position
     */
    private void deleteBook(int position) {
        // deleting the book from db
        db.deleteBook(bookList.get(position).getBook_id());

        // removing the book from the list
        bookList.remove(position);
        mAdapter.notifyItemRemoved(position);

        toggleEmptyNotes();
    }

    /**
     * Opens dialog with Edit - Delete options
     * Edit - 0
     * Delete - 0
     */
    private void showActionsDialog(final int position) {
        List<String> actions = new ArrayList<>();
        actions.add("View");
        //CharSequence actions[] = new CharSequence[]{"Edit", "Delete", "View"};

        if(bookList.get(position).getBook_status().toString().equals("AVAILABLE")){
            actions.add("Delete");
            actions.add("Edit");
            actions.add("Issue book");
            actions.add("Dispose book");
        }
        if(bookList.get(position).getBook_status().toString().equals("DISPOSED")){
            actions.add("Delete");
            actions.add("Edit");
        }
        if(bookList.get(position).getBook_status().toString().equals("LOANED"))
            actions.add("Return book");

        final CharSequence[] charActions = actions.toArray(new CharSequence[actions.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Choose option");
        builder.setItems(charActions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (charActions[which].equals("Edit")) {
                    editBook(position);
                }
                else if (charActions[which].equals("Delete")) {
                    deleteBook(position);
                }
                else if (charActions[which].equals("View")) {
                    viewBook(position);
                }
                else if (charActions[which].equals("Issue book")){
                    issueBook(position);
                }
                else if (charActions[which].equals("Return book")){
                    returnBook(position);
                }
                else if (charActions[which].equals("Dispose book")){
                    disposeBook(position);
                }
            }
        });
        builder.show();
    }

    private void disposeBook(int position) {
        Book book;
        int id = bookList.get(position).getBook_id();
        book = db.getBook(id);
        book.setBook_status(BookStatus.DISPOSED);
        db.updateBook(book);
        bookList.clear();
        bookList.addAll(db.getAllBooks(user_id));
        mAdapter.notifyDataSetChanged();
    }

    private void returnBook(int position) {
        Loan loan = new Loan();
        Book book;
        Reader reader = new Reader();
        int updated_number_f_copies = 0;

        //getting ids of chosen book and reader
        //getting number of copies and increment it

        int id = bookList.get(position).getBook_id();
        book = db.getBook(id);



        loan = db.getLoansByBookID(id);
        reader = new Reader(db.getReader(loan.getReader_id()));
        loan.setIf_closed(true);
        if(book!=null){
            book.setBook_status(BookStatus.AVAILABLE);
            db.updateBook(book);
        }
        if(reader!=null){
            reader.setHas_book(false);
            db.updateReader(reader);
        }
        db.updateLoan(loan);
        bookList.clear();
        bookList.addAll(db.getAllBooks(user_id));
        mAdapter.notifyDataSetChanged();
    }

    private void issueBook(int position) {
        Fragment newFragment = new IssueBookFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Bundle args = new Bundle();
        args.putInt("position", bookList.get(position).getBook_id());
        args.putString("user", user_id);

        newFragment.setArguments(args);

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack if needed
        transaction.replace(R.id.nav_host_fragment, newFragment);
        // Commit the transaction
        transaction.commit();
    }

    private void editBook(int position) {
        // Create new fragment and transaction
        Fragment newFragment = new EditBookFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Bundle args = new Bundle();
        args.putInt("position", bookList.get(position).getBook_id());
        newFragment.setArguments(args);

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack if needed
        transaction.replace(R.id.nav_host_fragment, newFragment);
        // Commit the transaction
        transaction.commit();
    }

    private void viewBook(int position) {
        // Create new fragment and transaction
        Fragment newFragment = new ViewBookFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Bundle args = new Bundle();
        args.putInt("position", bookList.get(position).getBook_id());
        newFragment.setArguments(args);

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack if needed
        transaction.replace(R.id.nav_host_fragment, newFragment);

        // Commit the transaction
        transaction.commit();
    }


    /**
     * Toggling list and empty notes view
     */
    private void toggleEmptyNotes() {
        // you can check notesList.size() > 0

        if (db.getBooksCount(user_id) > 0) {
            noNotesView.setVisibility(View.GONE);
        } else {
            noNotesView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
        inflater.inflate(R.menu.appbar_book_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.getFilter().filter(newText);
                return false;
            }
        });

    }

    public boolean onOptionsItemSelected(MenuItem item){
        ArrayList<Book> newBookList;
        switch (item.getItemId()){
            case R.id.item_show_available:
                newBookList = new ArrayList<>();
                bookList.clear();
                bookList.addAll(db.getAllBooks(user_id));
                for (Book b: bookList
                     ) {
                    if(b.getBook_status().toString().equals("AVAILABLE")){
                        newBookList.add(b);
                    }
                }
                bookList.retainAll(newBookList);
                mAdapter.notifyDataSetChanged();
                return true;
            case R.id.item_show_landed:
                newBookList = new ArrayList<>();
                bookList.clear();
                bookList.addAll(db.getAllBooks(user_id));
                for (Book b: bookList
                ) {
                    if(b.getBook_status().toString().equals("LOANED")){
                        newBookList.add(b);
                    }
                }
                bookList.retainAll(newBookList);
                mAdapter.notifyDataSetChanged();
                return true;
            case R.id.item_show_disposed:
                bookList.clear();
                bookList.addAll(db.getAllBooks(user_id));
                newBookList = new ArrayList<>();
                for (Book b: bookList
                ) {
                    if(b.getBook_status().toString().equals("DISPOSED")){
                        newBookList.add(b);
                    }
                }
                bookList.retainAll(newBookList);
                mAdapter.notifyDataSetChanged();
                return true;
            case R.id.item_show_all:
                bookList.clear();
                bookList.addAll(db.getAllBooks(user_id));
                mAdapter.notifyDataSetChanged();
                return true;
            case R.id.item_logout:
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(getActivity(), SignUpActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                getActivity().finish();
                return true;
            default:
                return false;
        }
    }
}