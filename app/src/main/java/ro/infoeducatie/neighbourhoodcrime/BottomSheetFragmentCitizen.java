package ro.infoeducatie.neighbourhoodcrime;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheetFragmentCitizen extends BottomSheetDialogFragment {
    String mTag;

    public static BottomSheetFragmentCitizen newInstance(String tag) {
        BottomSheetFragmentCitizen f = new BottomSheetFragmentCitizen();
        Bundle args = new Bundle();
        args.putString("TAG", tag);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTag = getArguments().getString("Tag");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_citizen, container, false);
        return view;
    }
}
