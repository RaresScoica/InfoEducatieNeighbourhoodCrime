package ro.infoeducatie.neighbourhoodcrime;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheetFragmentLawenforcer extends BottomSheetDialogFragment {
    String mTag;

    public static BottomSheetFragmentLawenforcer newInstance(String tag) {
        BottomSheetFragmentLawenforcer f = new BottomSheetFragmentLawenforcer();
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
        View view = inflater.inflate(R.layout.bottom_sheet_lawenforcer, container, false);
        return view;
    }
}
