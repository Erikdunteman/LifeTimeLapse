package com.erikdunteman.erik.lifetimelapse;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.erikdunteman.erik.lifetimelapse.models.Project;
import com.erikdunteman.erik.lifetimelapse.models.ProjectDB;
import com.erikdunteman.erik.lifetimelapse.models.User;
import com.erikdunteman.erik.lifetimelapse.utils.DatabaseHelper;
import com.erikdunteman.erik.lifetimelapse.utils.ProjectsMenuCVAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * Created by Erik on 1/17/2018.
 */

public class ProjectsMenuFragment extends Fragment {

    private static final String TAG = "ProjectsMenuFragment";

    //This will evade the nullpointer exception when adding to a new bundle from MainActivity.
    public ProjectsMenuFragment() {
        super();
        setArguments(new Bundle());
    }

    public interface OnProjectSelectedListener {
        void OnProjectSelected(Project con);
    }

    OnProjectSelectedListener mProjectListener;


    //Step 1 for sending info here to Project Add Fragment
    public interface OnProjectAddListener {
        public void onProjectAdd();
    }
    OnProjectAddListener mOnAddProject;



    //Variables and Widgets
    private static final int STANDARD_APPBAR = 1;
    private static final int SEARCH_APPBAR = 0;
    private int mAppBarState;

    private AppBarLayout viewProjectsMenuBar, searchBar;
    private ProjectsMenuCVAdapter adapter;
    private ListView projectsList;
    private EditText mSearchProjects;
    private TextView NoProjectPrompt;
    private ArrayList<Project> mProjects;
    private ArrayList<String> mProjectNames;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projectsmenu, container, false);
        viewProjectsMenuBar = view.findViewById(R.id.viewProjectsMenuToolbar);
        searchBar = view.findViewById(R.id.searchToolbar);
        projectsList = view.findViewById(R.id.lvProjectsList);
        NoProjectPrompt = view.findViewById(R.id.projectsmenuNoProjectPrompt);
        mSearchProjects = (EditText) view.findViewById(R.id.etSearchProjects);
        Log.d(TAG, "onCreateView: started");

            setAppBarState(STANDARD_APPBAR);

        //Pop Welcome from Backstack
        FragmentManager manager = getActivity().getSupportFragmentManager();
        manager.popBackStack("WelcomeOneFragment", FragmentManager.POP_BACK_STACK_INCLUSIVE);

        //Tool Bar Toggle Functionality - Identifying the Buttons
        //and activating toggleToolBarState method
        ImageView ivSearchProject = view.findViewById(R.id.ivSearchIcon);
        ivSearchProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked search icon");
                toggleToolBarState();
            }
        });
        ImageView ivBackArrow = view.findViewById(R.id.ivBackArrow);
        ivBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked back arrow.");
                toggleToolBarState();
            }
        });

        //FAB functionality
        FloatingActionButton FAB = view.findViewById(R.id.fabAddProject);
        FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked fab");
                //initiate new project add
//                init_f_pa();

                //Step 5 to add project into the bundle and call navigation
                mOnAddProject.onProjectAdd();
            }
        });


        setupProjectsList();
        Log.d(TAG, "onCreateView: Project List Set Up");
        

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mProjectListener = (OnProjectSelectedListener) getActivity();

            //Step 2 for sending info here to Project Add Fragment
            mOnAddProject = (OnProjectAddListener) getActivity();


        } catch (ClassCastException e) {
            Log.e(TAG, "onAttach: ClassCastException " + e.getMessage());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupProjectsList();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        for(int i =0; i > mProjects.size(); i++){
            outState.putParcelable(("Project" + i), mProjects.get(i));
        }
    }


    private Project getProjectFromBundle() {
        Log.d(TAG, "getProjectFromBundle: arguments " + getArguments());

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            return bundle.getParcelable("NewProject");
        } else {
            return null;
        }
    }


    private void init_f_pi() {
        ProjectInfoFragment fragment = new ProjectInfoFragment();
        android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        // replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void init_f_pa() {
        ProjectAddFragment fragment = new ProjectAddFragment();
        android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        // replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }


    private void setupProjectsList() {
        final ArrayList<Project> projects = getProjectsFromDB();
        mProjects = projects;
        Log.d(TAG, "setupProjectsList:"+ projects.toString());;


        //sort the arraylist based on project name
        Collections.sort(projects, new Comparator<Project>() {
            @Override
            public int compare(Project o1, Project o2) {
                return o1.getProjName().compareToIgnoreCase(o2.getProjName());
            }
        });


        mSearchProjects.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String text = mSearchProjects.getText().toString().toLowerCase(Locale.getDefault());
                if(adapter!=null){
                    adapter.filter(text);
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });


        projectsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Log.d(TAG, "onClick: navigating to " + getString(R.string.project_info_fragment));

                //pass the contact to the interface and send to MainActivity
                mProjectListener.OnProjectSelected(projects.get(position));
            }
        });

    }














    private ArrayList<Project> getProjectsFromDB() {
        final ArrayList<Project> projects = new ArrayList<Project>();


        //Get the user's project names

       //New test method
        Project project1 = new Project("Landscape Test","","","","1");
        Project project2 = new Project("Landscape Test 2","","","","2");
        Project project3 = new Project("Landscape Test 3","","","","3");

        projects.add(project1);
        projects.add(project2);
        projects.add(project3);

        Log.d(TAG, "getProjectsFromDB: projects manually added: " + projects.toString());

        Log.d(TAG, "getProjectsFromDB: Attempting to add DB projects");
        DatabaseHelper databaseHelper = new DatabaseHelper(getActivity());
        Cursor cursor = databaseHelper.getAllProjects();
        if(!cursor.moveToNext()){
            Toast.makeText(getActivity(), "There are no projects to show", Toast.LENGTH_SHORT).show();
        }

        while (cursor.moveToNext()){
            projects.add(new Project(
                    cursor.getString(1), //name
                    cursor.getString(2), //freq
                    cursor.getString(3), //length
                    cursor.getString(4), //length goal
                    cursor.getString(5) //photo tag
            ));
        }
        Log.d(TAG, "getProjectsFromDB: projects, manual and DB: " + projects.toString());

        if (projects == null){
            NoProjectPrompt.setVisibility(View.VISIBLE);
        } else{
            NoProjectPrompt.setVisibility(View.GONE);
        }

        adapter = new ProjectsMenuCVAdapter(getActivity(), R.layout.layout_projectscardview, projects, "");
        projectsList.setAdapter(adapter);
        return projects;
    }



    //initiates appbar state toggle
    private void toggleToolBarState() {
        Log.d(TAG, "toggleToolBarState: toggling AppBarState");
        if (mAppBarState == STANDARD_APPBAR) {
            setAppBarState(SEARCH_APPBAR);
        } else {
            setAppBarState(STANDARD_APPBAR);
        }
    }

    //Sets Appbar state for either search mode or standard mode
    private void setAppBarState(int state) {
        Log.d(TAG, "setAppBarState: changing app bar state to " + state);
        mAppBarState = state;
        if (mAppBarState == SEARCH_APPBAR) {
            viewProjectsMenuBar.setVisibility(View.GONE);
            searchBar.setVisibility(View.VISIBLE);
            //Open Keyboard
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        } else if (mAppBarState == STANDARD_APPBAR) {
            searchBar.setVisibility(View.GONE);
            viewProjectsMenuBar.setVisibility(View.VISIBLE);
            View view = getView();
            //Close Keyboard
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            try {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            } catch (NullPointerException e) {
                Log.d(TAG, "setAppBarState: NullPointerException: " + e.getMessage());
            }
        }
    }

}
