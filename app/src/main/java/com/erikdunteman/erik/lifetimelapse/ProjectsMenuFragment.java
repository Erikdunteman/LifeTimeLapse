package com.erikdunteman.erik.lifetimelapse;

import android.content.Context;
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

import com.erikdunteman.erik.lifetimelapse.models.Project;
import com.erikdunteman.erik.lifetimelapse.models.ProjectDB;
import com.erikdunteman.erik.lifetimelapse.models.User;
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
        Log.d(TAG, "setupProjectsList: Getting Project Names (timestamps) from User Node");
        final String uID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(uID);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Log.d(TAG, "onDataChange: entered");
                Log.d(TAG, "onDataChange: datasnapshot: " + dataSnapshot.toString());
                //Testing to see if project list exists
                if(dataSnapshot.getValue(User.class)!=null && dataSnapshot.getValue(User.class).getProjectNames()!=null){
                    NoProjectPrompt.setVisibility(View.GONE);
                    User user = dataSnapshot.getValue(User.class);

                    Log.d(TAG, "onDataChange: found user: "
                            + user.toString());
                    ArrayList<String> projecttimestamps = user.getProjectNames();
                    Log.d(TAG, "onDataChange: recovered projectlist: " + projecttimestamps.toString());

                    //Now to use those projecttimestamps for getting and adding full project info to projects list
                    Log.d(TAG, "onDataChange: Iterating through projects with the timestamps and adding to list for listview");
                    for(final String projecttimestamp : projecttimestamps){
                        //Use that projecttimestamp to identify individual projects.
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                                .child("projects")
                                .child(uID)
                                .child(projecttimestamp);
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                ProjectDB project = dataSnapshot.getValue(ProjectDB.class);
                                if (project != null){
                                    Project proj = new Project(project.getProjName(), project.getProjFreq(), project.getProjLength(), project.getProjLengthGoal(), project.getProjPhotoTag());
                                    Log.d(TAG, "onDataChange: Project loaded, to string: " + proj.toString());
                                    projects.add(proj);
                                    Log.d(TAG, "setupProjectsList: Resetting Adapter after adding: proj = " + proj.toString());
                                }


                                adapter = new ProjectsMenuCVAdapter(getActivity(), R.layout.layout_projectscardview, mProjects, "");
                                projectsList.setAdapter(adapter);
                            }
                            @Override public void onCancelled(DatabaseError databaseError) {
                            }
                        });
                    }
                }else { //the projectlist is null
                    Log.d(TAG, "onDataChange: recovered project list: user has no projects");
                    NoProjectPrompt.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

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
